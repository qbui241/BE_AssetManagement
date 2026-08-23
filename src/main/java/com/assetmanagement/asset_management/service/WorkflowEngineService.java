package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.entity.ApprovalStep;
import com.assetmanagement.asset_management.entity.ApprovalWorkflow;
import com.assetmanagement.asset_management.entity.Asset;
import com.assetmanagement.asset_management.repository.ApprovalStepRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WorkflowEngineService {

    private final ApprovalStepRepository approvalStepRepository;

    public WorkflowEngineService(
            ApprovalStepRepository approvalStepRepository) {
        this.approvalStepRepository = approvalStepRepository;
    }

    public ApprovalStep findFirstApplicableStep(
            Asset asset,
            ApprovalWorkflow workflow) {

        List<ApprovalStep> steps =
                approvalStepRepository
                        .findByWorkflowIdOrderByStepOrderAsc(
                                workflow.getId()
                        );

        for (ApprovalStep step : steps) {

            if (isStepApplicable(step, asset.getValue())) {
                return step;
            }
        }

        return null;
    }

    private boolean isStepApplicable(
            ApprovalStep step,
            BigDecimal assetValue) {

        if (step.getMinValue() != null
                && assetValue.compareTo(step.getMinValue()) < 0) {
            return false;
        }

        if (step.getMaxValue() != null
                && assetValue.compareTo(step.getMaxValue()) > 0) {
            return false;
        }

        return true;
    }
}