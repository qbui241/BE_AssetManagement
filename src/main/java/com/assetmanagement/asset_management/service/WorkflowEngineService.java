package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.AssetAssignmentRequest;
import com.assetmanagement.asset_management.entity.*;
import com.assetmanagement.asset_management.enums.*;
import com.assetmanagement.asset_management.exception.InvalidStatusTransitionException;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkflowEngineService {

    private final ApprovalWorkflowRepository approvalWorkflowRepository;
    private final ApprovalStepRepository approvalStepRepository;
    private final ApprovalTaskRepository approvalTaskRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final AssetRepository assetRepository;
    private final AssetService assetService;

    public WorkflowEngineService(
            ApprovalWorkflowRepository approvalWorkflowRepository,
            ApprovalStepRepository approvalStepRepository,
            ApprovalTaskRepository approvalTaskRepository,
            ApprovalRequestRepository approvalRequestRepository,
            AssetRepository assetRepository,
            AssetService assetService) {
        this.approvalWorkflowRepository = approvalWorkflowRepository;
        this.approvalStepRepository = approvalStepRepository;
        this.approvalTaskRepository = approvalTaskRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.assetRepository = assetRepository;
        this.assetService = assetService;
    }

    public ApprovalWorkflow findActiveWorkflow(ActionType actionType) {
        return approvalWorkflowRepository.findByActionTypeAndActiveTrue(actionType)
                .orElseThrow(() -> new IllegalStateException(
                        "No active approval workflow found for action: " + actionType
                ));
    }

    public List<ApprovalStep> findApplicableSteps(Asset asset, ApprovalWorkflow workflow) {
        List<ApprovalStep> steps = approvalStepRepository
                .findByWorkflowIdOrderByStepOrderAsc(workflow.getId());

        return steps.stream()
                .filter(step -> isStepApplicable(step, asset.getValue()))
                .toList();
    }

    private boolean isStepApplicable(ApprovalStep step, BigDecimal assetValue) {
        if (step.getMinValue() != null && assetValue.compareTo(step.getMinValue()) < 0) {
            return false;
        }
        if (step.getMaxValue() != null && assetValue.compareTo(step.getMaxValue()) > 0) {
            return false;
        }
        return true;
    }

    @Transactional
    public void createInitialTasks(ApprovalRequest approvalRequest) {
        ApprovalWorkflow workflow = approvalRequest.getWorkflow();
        List<ApprovalStep> applicableSteps = findApplicableSteps(approvalRequest.getAsset(), workflow);

        if (applicableSteps.isEmpty()) {
            throw new IllegalStateException("No applicable approval step found");
        }

        if (workflow.getType() == WorkflowType.SEQUENTIAL) {
            createSequentialFirstTask(approvalRequest, applicableSteps);
        } else if (workflow.getType() == WorkflowType.PARALLEL) {
            createParallelTasks(approvalRequest, applicableSteps);
        }
    }

    private void createSequentialFirstTask(ApprovalRequest approvalRequest, List<ApprovalStep> applicableSteps) {
        ApprovalStep firstStep = applicableSteps.get(0);
        approvalRequest.setCurrentStepOrder(firstStep.getStepOrder());

        Department targetDept = resolveTargetDepartment(firstStep, approvalRequest);
        Branch targetBranch = resolveTargetBranch(firstStep, approvalRequest);

        ApprovalTask task = ApprovalTask.builder()
                .approvalRequest(approvalRequest)
                .role(firstStep.getRole())
                .department(targetDept)
                .branch(targetBranch)
                .stepOrder(firstStep.getStepOrder())
                .status(ApprovalRequestStatus.PENDING)
                .build();

        approvalTaskRepository.save(task);
    }

    private void createParallelTasks(ApprovalRequest approvalRequest, List<ApprovalStep> applicableSteps) {
        for (ApprovalStep step : applicableSteps) {
            Department targetDept = resolveTargetDepartment(step, approvalRequest);
            Branch targetBranch = resolveTargetBranch(step, approvalRequest);

            ApprovalTask task = ApprovalTask.builder()
                    .approvalRequest(approvalRequest)
                    .role(step.getRole())
                    .department(targetDept)
                    .branch(targetBranch)
                    .stepOrder(step.getStepOrder())
                    .status(ApprovalRequestStatus.PENDING)
                    .build();

            approvalTaskRepository.save(task);
        }
    }

    @Transactional
    public void processTaskDecision(ApprovalTask currentTask) {
        ApprovalRequest approvalRequest = currentTask.getApprovalRequest();
        ApprovalWorkflow workflow = approvalRequest.getWorkflow();

        if (workflow.getType() == WorkflowType.SEQUENTIAL) {
            if (currentTask.getStatus() == ApprovalRequestStatus.REJECTED) {
                terminateApprovalRequest(approvalRequest, ApprovalRequestStatus.CANCELLED);
            } else {
                processSequentialNextStep(currentTask, approvalRequest, workflow);
            }
        } else if (workflow.getType() == WorkflowType.PARALLEL) {
            if (currentTask.getStatus() == ApprovalRequestStatus.REJECTED) {
                String reason = buildRejectionCascadeNote(currentTask);

                approvalTaskRepository.cancelPendingSiblingTasks(
                        approvalRequest.getId(),
                        currentTask.getStepOrder(),
                        reason
                );
                terminateApprovalRequest(approvalRequest, ApprovalRequestStatus.CANCELLED);
            } else {
                processParallelEvaluation(currentTask.getStepOrder(), approvalRequest);
            }
        }
    }

    private String buildRejectionCascadeNote(ApprovalTask rejectedTask) {
        String rejectedBy = rejectedTask.getDepartment() != null
                ? rejectedTask.getDepartment().getName()
                : rejectedTask.getRole().getName();

        return "Đã huỷ do " + rejectedBy + " từ chối";
    }

    private void processSequentialNextStep(
            ApprovalTask approvedTask,
            ApprovalRequest approvalRequest,
            ApprovalWorkflow workflow) {

        List<ApprovalStep> applicableSteps = findApplicableSteps(approvalRequest.getAsset(), workflow);
        Integer fromStepOrder = approvedTask.getStepOrder();

        while (true) {
            Integer finalFromStepOrder = fromStepOrder;
            ApprovalStep nextStep = applicableSteps.stream()
                    .filter(step -> step.getStepOrder() > finalFromStepOrder)
                    .findFirst()
                    .orElse(null);

            if (nextStep == null) {
                completeApprovalRequest(approvalRequest);
                return;
            }

            boolean shouldAutoSkip = nextStep.getDepartmentScope() == DepartmentScope.ASSET_DEPARTMENT
                    && approvalRequest.getRequester().getDepartment().getId()
                    .equals(approvalRequest.getAsset().getDepartment().getId());

            if (shouldAutoSkip) {
                fromStepOrder = nextStep.getStepOrder();
                continue;
            }

            Department targetDept = resolveTargetDepartment(nextStep, approvalRequest);
            Branch targetBranch = resolveTargetBranch(nextStep, approvalRequest);

            approvalRequest.setCurrentStepOrder(nextStep.getStepOrder());
            ApprovalTask nextTask = ApprovalTask.builder()
                    .approvalRequest(approvalRequest)
                    .role(nextStep.getRole())
                    .department(targetDept)
                    .branch(targetBranch)
                    .stepOrder(nextStep.getStepOrder())
                    .status(ApprovalRequestStatus.PENDING)
                    .build();

            approvalTaskRepository.save(nextTask);
            return;
        }
    }

    private void processParallelEvaluation(Integer currentStepOrder, ApprovalRequest approvalRequest) {
        boolean hasPending = approvalTaskRepository.existsByApprovalRequestIdAndStepOrderAndStatus(
                approvalRequest.getId(),
                currentStepOrder,
                ApprovalRequestStatus.PENDING
        );

        if (hasPending) {
            return;
        }

        completeApprovalRequest(approvalRequest);
    }

    private Department resolveTargetDepartment(ApprovalStep step, ApprovalRequest request) {
        return switch (step.getDepartmentScope()) {
            case REQUESTER_DEPARTMENT -> request.getRequester().getDepartment();
            case ASSET_DEPARTMENT -> request.getAsset().getDepartment();
            case SPECIFIC_DEPARTMENT -> step.getDepartment();
            default -> null;
        };
    }

    private Branch resolveTargetBranch(ApprovalStep step, ApprovalRequest request) {
        return switch (step.getDepartmentScope()) {
            case REQUESTER_BRANCH -> request.getRequester().getDepartment().getBranch();
            case ASSET_BRANCH -> request.getAsset().getDepartment().getBranch();
            case SPECIFIC_BRANCH -> step.getBranch();
            default -> null;
        };
    }

    private void terminateApprovalRequest(ApprovalRequest approvalRequest, ApprovalRequestStatus finalStatus) {
        approvalRequest.setStatus(finalStatus);
        approvalRequest.setCompletedAt(LocalDateTime.now());
        approvalRequestRepository.save(approvalRequest);
    }

    @Transactional
    public void completeApprovalRequest(ApprovalRequest approvalRequest) {
        Long assetId = approvalRequest.getAsset().getId();

        Asset asset = assetRepository.findByIdForUpdate(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));

        ActionType actionType = approvalRequest.getWorkflow().getActionType();

        if (actionType == ActionType.ASSIGNMENT) {
            if (asset.getStatus() != AssetStatus.AVAILABLE) {
                approvalRequest.setStatus(ApprovalRequestStatus.CANCELLED);
                approvalRequest.setCompletedAt(LocalDateTime.now());
                approvalRequestRepository.save(approvalRequest);
                throw new InvalidStatusTransitionException("Asset has already been assigned to another request");
            }

            assetService.assignAsset(
                    asset.getId(),
            new AssetAssignmentRequest(approvalRequest.getRequester().getId())
            );

            approvalRequest.setStatus(ApprovalRequestStatus.APPROVED);
            approvalRequest.setCompletedAt(LocalDateTime.now());
            approvalRequestRepository.save(approvalRequest);

            cancelCompetingRequests(approvalRequest.getId(), asset.getId());

        } else if (actionType == ActionType.DISPOSAL) {
            assetService.disposeAsset(asset.getId());
            approvalRequest.setStatus(ApprovalRequestStatus.APPROVED);
            approvalRequest.setCompletedAt(LocalDateTime.now());
            approvalRequestRepository.save(approvalRequest);
        }
    }

    private void cancelCompetingRequests(Long winningRequestId, Long assetId) {
        List<ApprovalRequest> competingRequests = approvalRequestRepository
                .findByAssetIdAndStatusAndIdNot(assetId, ApprovalRequestStatus.PENDING, winningRequestId);

        for (ApprovalRequest competing : competingRequests) {
            competing.setStatus(ApprovalRequestStatus.CANCELLED);
            competing.setCompletedAt(LocalDateTime.now());
            approvalRequestRepository.save(competing);

            // Hủy toàn bộ Task chưa duyệt của các request cạnh tranh
            approvalTaskRepository.rejectAllPendingTasksByRequestId(competing.getId());

        }
    }
}