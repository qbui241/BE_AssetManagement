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
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AssetService assetService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public WorkflowEngineService(
            ApprovalWorkflowRepository approvalWorkflowRepository,
            ApprovalStepRepository approvalStepRepository,
            ApprovalTaskRepository approvalTaskRepository,
            ApprovalRequestRepository approvalRequestRepository,
            AssetRepository assetRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            AssetService assetService,
            AuditLogService auditLogService,
            NotificationService notificationService) {
        this.approvalWorkflowRepository = approvalWorkflowRepository;
        this.approvalStepRepository = approvalStepRepository;
        this.approvalTaskRepository = approvalTaskRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.assetService = assetService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    public ApprovalWorkflow findActiveWorkflow(ActionType actionType) {
        return approvalWorkflowRepository.findByActionTypeAndActiveTrue(actionType)
                .orElseThrow(() -> new IllegalStateException(
                        "No active approval workflow found for action: " + actionType
                ));
    }

    public List<ApprovalStep> findApplicableSteps(ApprovalRequest approvalRequest, ApprovalWorkflow workflow) {
        List<ApprovalStep> steps = approvalStepRepository
                .findByWorkflowIdOrderByStepOrderAsc(workflow.getId());

        BigDecimal totalValue = resolveTotalValue(approvalRequest);

        return steps.stream()
                .filter(step -> isStepApplicable(step, totalValue))
                .toList();
    }

    private BigDecimal resolveTotalValue(ApprovalRequest approvalRequest) {
        Asset asset = approvalRequest.getAsset();

        if (asset.getTrackingType() == AssetTrackingType.BULK
                && approvalRequest.getRequestedQuantity() != null) {
            return asset.getValue().multiply(BigDecimal.valueOf(approvalRequest.getRequestedQuantity()));
        }

        return asset.getValue();
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
        List<ApprovalStep> applicableSteps = findApplicableSteps(approvalRequest, workflow);

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
        notifyEligibleApprovers(task, approvalRequest);
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
            notifyEligibleApprovers(task, approvalRequest);
        }
    }

    @Transactional
    public void processTaskDecision(ApprovalTask currentTask) {
        ApprovalRequest approvalRequest = currentTask.getApprovalRequest();
        ApprovalWorkflow workflow = approvalRequest.getWorkflow();

        auditLogService.log(
                "APPROVAL_TASK",
                currentTask.getId(),
                currentTask.getStatus().name(),
                "PENDING",
                currentTask.getStatus().name(),
                "Task step " + currentTask.getStepOrder() + " (" + currentTask.getRole().getName() + ") "
                        + (currentTask.getStatus() == ApprovalRequestStatus.APPROVED ? "được duyệt" : "bị từ chối"),
                currentTask.getApprovedBy()
        );

        if (workflow.getType() == WorkflowType.SEQUENTIAL) {
            if (currentTask.getStatus() == ApprovalRequestStatus.REJECTED) {
                terminateApprovalRequest(approvalRequest, ApprovalRequestStatus.CANCELLED);
                notificationService.notify(
                        approvalRequest.getRequester(),
                        "Yêu cầu bị từ chối",
                        "Yêu cầu #" + approvalRequest.getId() + " cho tài sản "
                                + approvalRequest.getAsset().getAssetCode() + " đã bị từ chối ở bước "
                                + currentTask.getStepOrder() + ".",
                        "APPROVAL_REQUEST",
                        approvalRequest.getId()
                );
            } else {
                processSequentialNextStep(currentTask, approvalRequest, workflow);
            }
        } else if (workflow.getType() == WorkflowType.PARALLEL) {
            if (currentTask.getStatus() == ApprovalRequestStatus.REJECTED) {
                String reason = buildRejectionCascadeNote(currentTask);

                notifyOtherApproversInStep(currentTask, approvalRequest, reason);

                approvalTaskRepository.cancelPendingSiblingTasks(
                        approvalRequest.getId(),
                        currentTask.getStepOrder(),
                        reason
                );
                terminateApprovalRequest(approvalRequest, ApprovalRequestStatus.CANCELLED);

                notificationService.notify(
                        approvalRequest.getRequester(),
                        "Yêu cầu bị từ chối",
                        "Yêu cầu #" + approvalRequest.getId() + " cho tài sản "
                                + approvalRequest.getAsset().getAssetCode() + " " + reason.toLowerCase() + ".",
                        "APPROVAL_REQUEST",
                        approvalRequest.getId()
                );
            } else {
                processParallelEvaluation(currentTask.getStepOrder(), approvalRequest);
            }
        }
    }

    private void notifyOtherApproversInStep(
            ApprovalTask rejectedTask,
            ApprovalRequest approvalRequest,
            String reason) {

        List<ApprovalTask> siblingTasks = approvalTaskRepository
                .findByApprovalRequestId(approvalRequest.getId())
                .stream()
                .filter(t -> t.getStepOrder().equals(rejectedTask.getStepOrder()))
                .filter(t -> !t.getId().equals(rejectedTask.getId()))
                .filter(t -> t.getStatus() == ApprovalRequestStatus.PENDING)
                .toList();

        for (ApprovalTask sibling : siblingTasks) {
            List<User> approvers = filterOutRequester(
                    findEligibleApproversFor(sibling), approvalRequest
            );
            for (User approver : approvers) {
                notificationService.notify(
                        approver,
                        "Yêu cầu duyệt đã bị huỷ",
                        reason + " - yêu cầu #" + approvalRequest.getId() + " không cần bạn duyệt nữa.",
                        "APPROVAL_TASK",
                        sibling.getId()
                );
            }
        }
    }

    private List<User> findEligibleApproversFor(ApprovalTask task) {
        Long departmentId = task.getDepartment() != null ? task.getDepartment().getId() : null;
        Long branchId = task.getBranch() != null ? task.getBranch().getId() : null;

        return userRepository.findEligibleApprovers(task.getRole().getId(), departmentId, branchId);
    }

    private void notifyEligibleApprovers(ApprovalTask task, ApprovalRequest approvalRequest) {
        List<User> approvers = filterOutRequester(
                findEligibleApproversFor(task), approvalRequest
        );

        if (approvers.isEmpty()) {
            approvers = escalateTask(task, approvalRequest);
        }

        for (User approver : approvers) {
            notificationService.notify(
                    approver,
                    "Có yêu cầu mới cần duyệt",
                    "Yêu cầu #" + approvalRequest.getId() + " cho tài sản "
                            + approvalRequest.getAsset().getAssetCode() + " đang chờ bạn duyệt (bước "
                            + task.getStepOrder() + ").",
                    "APPROVAL_TASK",
                    task.getId()
            );
        }
    }

    private List<User> escalateTask(ApprovalTask task, ApprovalRequest approvalRequest) {
        String fromRole = task.getRole().getName();

        if ("MANAGER".equals(fromRole)) {
            Branch branch = task.getDepartment() != null
                    ? task.getDepartment().getBranch()
                    : task.getBranch();

            if (branch != null) {
                List<User> directors = escalateTo(
                        task, approvalRequest, "DIRECTOR", null, branch, fromRole
                );
                if (!directors.isEmpty()) {
                    return directors;
                }
                fromRole = "DIRECTOR";
            }
        }

        if (!"ADMIN".equals(fromRole)) {
            return escalateTo(task, approvalRequest, "ADMIN", null, null, fromRole);
        }

        return List.of();
    }

    private List<User> escalateTo(
            ApprovalTask task,
            ApprovalRequest approvalRequest,
            String roleName,
            Department department,
            Branch branch,
            String fromRoleName) {

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException(
                        "Role " + roleName + " chưa được cấu hình trong hệ thống"
                ));

        task.setRole(role);
        task.setDepartment(department);
        task.setBranch(branch);
        ApprovalTask savedTask = approvalTaskRepository.save(task);

        auditLogService.log(
                "APPROVAL_TASK",
                savedTask.getId(),
                "ESCALATED",
                fromRoleName,
                roleName,
                "Không còn ai đủ điều kiện duyệt ở cấp " + fromRoleName
                        + " (ngoại trừ chính người yêu cầu) nên hệ thống tự động"
                        + " chuyển task lên cấp " + roleName + ".",
                null
        );

        return filterOutRequester(findEligibleApproversFor(savedTask), approvalRequest);
    }

    private List<User> filterOutRequester(List<User> users, ApprovalRequest approvalRequest) {
        Long requesterId = approvalRequest.getRequester().getId();
        return users.stream()
                .filter(u -> !u.getId().equals(requesterId))
                .toList();
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

        List<ApprovalStep> applicableSteps = findApplicableSteps(approvalRequest, workflow);
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
            notifyEligibleApprovers(nextTask, approvalRequest);
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
            if (asset.getTrackingType() == AssetTrackingType.INDIVIDUAL) {
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
                cancelCompetingRequests(approvalRequest.getId(), asset.getId());
            } else {
                Integer requestedQuantity = approvalRequest.getRequestedQuantity();
                if (asset.getAvailableQuantity() == null || asset.getAvailableQuantity() < requestedQuantity) {
                    approvalRequest.setStatus(ApprovalRequestStatus.CANCELLED);
                    approvalRequest.setCompletedAt(LocalDateTime.now());
                    approvalRequestRepository.save(approvalRequest);
                    throw new InvalidStatusTransitionException(
                            "Not enough available quantity left: requested " + requestedQuantity
                                    + " but only " + asset.getAvailableQuantity() + " available"
                    );
                }

                assetService.assignQuantity(
                        asset.getId(),
                        approvalRequest.getRequester().getId(),
                        requestedQuantity
                );
            }

            approvalRequest.setStatus(ApprovalRequestStatus.APPROVED);
            approvalRequest.setCompletedAt(LocalDateTime.now());
            approvalRequestRepository.save(approvalRequest);

            auditLogService.log(
                    "APPROVAL_REQUEST",
                    approvalRequest.getId(),
                    "APPROVED",
                    "PENDING",
                    "APPROVED",
                    "Yêu cầu cấp phát tài sản " + asset.getAssetCode() + " đã được duyệt xong",
                    null
            );

            notificationService.notify(
                    approvalRequest.getRequester(),
                    "Yêu cầu đã được duyệt",
                    "Tài sản " + asset.getAssetCode() + " đã được cấp phát cho bạn.",
                    "APPROVAL_REQUEST",
                    approvalRequest.getId()
            );

        } else if (actionType == ActionType.DISPOSAL) {
            if (asset.getTrackingType() == AssetTrackingType.INDIVIDUAL) {
                assetService.disposeAsset(asset.getId());
            } else {
                Integer requestedQuantity = approvalRequest.getRequestedQuantity();
                if (asset.getAvailableQuantity() == null || asset.getAvailableQuantity() < requestedQuantity) {
                    approvalRequest.setStatus(ApprovalRequestStatus.CANCELLED);
                    approvalRequest.setCompletedAt(LocalDateTime.now());
                    approvalRequestRepository.save(approvalRequest);
                    throw new InvalidStatusTransitionException(
                            "Not enough available quantity left to dispose: requested " + requestedQuantity
                                    + " but only " + asset.getAvailableQuantity() + " available"
                    );
                }

                assetService.disposeQuantity(asset.getId(), requestedQuantity);
            }

            approvalRequest.setStatus(ApprovalRequestStatus.APPROVED);
            approvalRequest.setCompletedAt(LocalDateTime.now());
            approvalRequestRepository.save(approvalRequest);

            auditLogService.log(
                    "APPROVAL_REQUEST",
                    approvalRequest.getId(),
                    "APPROVED",
                    "PENDING",
                    "APPROVED",
                    "Yêu cầu thanh lý tài sản " + asset.getAssetCode() + " đã được duyệt xong",
                    null
            );

            notificationService.notify(
                    approvalRequest.getRequester(),
                    "Yêu cầu đã được duyệt",
                    "Tài sản " + asset.getAssetCode() + " đã được thanh lý theo yêu cầu của bạn.",
                    "APPROVAL_REQUEST",
                    approvalRequest.getId()
            );
        }
    }

    private void cancelCompetingRequests(Long winningRequestId, Long assetId) {
        List<ApprovalRequest> competingRequests = approvalRequestRepository
                .findByAssetIdAndStatusAndIdNot(assetId, ApprovalRequestStatus.PENDING, winningRequestId);

        for (ApprovalRequest competing : competingRequests) {
            competing.setStatus(ApprovalRequestStatus.CANCELLED);
            competing.setCompletedAt(LocalDateTime.now());
            approvalRequestRepository.save(competing);

            approvalTaskRepository.rejectAllPendingTasksByRequestId(competing.getId());

            auditLogService.log(
                    "APPROVAL_REQUEST",
                    competing.getId(),
                    "CANCELLED",
                    "PENDING",
                    "CANCELLED",
                    "Yêu cầu bị huỷ do tài sản " + competing.getAsset().getAssetCode()
                            + " đã được gán cho yêu cầu #" + winningRequestId,
                    null
            );

            notificationService.notify(
                    competing.getRequester(),
                    "Yêu cầu đã bị huỷ",
                    "Tài sản " + competing.getAsset().getAssetCode()
                            + " bạn yêu cầu đã được cấp cho người khác trước, yêu cầu của bạn đã bị huỷ.",
                    "APPROVAL_REQUEST",
                    competing.getId()
            );
        }
    }
}