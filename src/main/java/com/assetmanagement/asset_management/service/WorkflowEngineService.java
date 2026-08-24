package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.entity.*;
import com.assetmanagement.asset_management.enums.ApprovalRequestStatus;
import com.assetmanagement.asset_management.repository.ApprovalStepRepository;
import com.assetmanagement.asset_management.repository.ApprovalTaskRepository;
import com.assetmanagement.asset_management.repository.ApprovalWorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WorkflowEngineService {

    private final ApprovalWorkflowRepository approvalWorkflowRepository;
    private final ApprovalStepRepository approvalStepRepository;
    private final ApprovalTaskRepository approvalTaskRepository;

    public WorkflowEngineService(
            ApprovalWorkflowRepository approvalWorkflowRepository,
            ApprovalStepRepository approvalStepRepository,
            ApprovalTaskRepository approvalTaskRepository) {

        this.approvalWorkflowRepository = approvalWorkflowRepository;
        this.approvalStepRepository = approvalStepRepository;
        this.approvalTaskRepository = approvalTaskRepository;
    }

    public ApprovalWorkflow findActiveWorkflow() {

        return approvalWorkflowRepository.findFirstByActiveTrue()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No active approval workflow found"
                        ));
    }

    public List<ApprovalStep> findApplicableSteps(
            Asset asset,
            ApprovalWorkflow workflow) {

        List<ApprovalStep> steps =
                approvalStepRepository
                        .findByWorkflowIdOrderByStepOrderAsc(
                                workflow.getId()
                        );

        return steps.stream()
                .filter(step -> isStepApplicable(step, asset.getValue()))
                .toList();
    }

    public ApprovalStep findFirstApplicableStep(
            Asset asset,
            ApprovalWorkflow workflow) {

        List<ApprovalStep> applicableSteps =
                findApplicableSteps(asset, workflow);

        if (applicableSteps.isEmpty()) {
            throw new IllegalStateException(
                    "No applicable approval step found"
            );
        }

        return applicableSteps.get(0);
    }

    private boolean isStepApplicable(
            ApprovalStep step,
            BigDecimal assetValue) {

        if (step.getMinValue() != null && assetValue.compareTo(step.getMinValue()) < 0) {
            return false;
        }

        if (step.getMaxValue() != null && assetValue.compareTo(step.getMaxValue()) > 0) {
            return false;
        }
        return true;
    }

    @Transactional
    public ApprovalTask createFirstTask(
            ApprovalRequest approvalRequest) {

        ApprovalStep firstStep =
                findFirstApplicableStep(
                        approvalRequest.getAsset(),
                        approvalRequest.getWorkflow()
                );

        ApprovalTask task = ApprovalTask.builder()
                .approvalRequest(approvalRequest)
                .role(firstStep.getRole())
                .stepOrder(firstStep.getStepOrder())
                .status(ApprovalRequestStatus.PENDING)
                .build();

        return approvalTaskRepository.save(task);
    }

    @Transactional
    public void processApprovedTask(ApprovalTask approvedTask) {

        ApprovalRequest approvalRequest = approvedTask.getApprovalRequest();
        ApprovalWorkflow workflow = approvalRequest.getWorkflow();

        List<ApprovalStep> applicableSteps = findApplicableSteps(
                        approvalRequest.getAsset(),
                        workflow
                );

        ApprovalStep nextStep = applicableSteps.stream()
                .filter(step ->
                        step.getStepOrder() > approvedTask.getStepOrder()
                )
                .findFirst()
                .orElse(null);

        if (nextStep == null) {
            approvalRequest.setStatus(
                    ApprovalRequestStatus.APPROVED
            );
            return;
        }

        approvalRequest.setCurrentStepOrder(nextStep.getStepOrder());

        ApprovalTask nextTask = ApprovalTask.builder()
                .approvalRequest(approvalRequest)
                .role(nextStep.getRole())
                .stepOrder(nextStep.getStepOrder())
                .status(ApprovalRequestStatus.PENDING)
                .build();

        approvalTaskRepository.save(nextTask);
    }

}