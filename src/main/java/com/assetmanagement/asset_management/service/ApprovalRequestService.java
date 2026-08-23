package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.ApprovalRequestRequest;
import com.assetmanagement.asset_management.entity.*;
import com.assetmanagement.asset_management.enums.ApprovalRequestStatus;
import com.assetmanagement.asset_management.enums.AssetStatus;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ApprovalRequestService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final ApprovalWorkflowRepository approvalWorkflowRepository;
    private final WorkflowEngineService workflowEngineService;

    public ApprovalRequestService(
            ApprovalRequestRepository approvalRequestRepository,
            AssetRepository assetRepository,
            UserRepository userRepository,
            ApprovalWorkflowRepository approvalWorkflowRepository,
            WorkflowEngineService workflowEngineService) {

        this.approvalRequestRepository = approvalRequestRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.approvalWorkflowRepository = approvalWorkflowRepository;
        this.workflowEngineService = workflowEngineService;
    }

    @Transactional
    public ApprovalRequest createRequest(
            ApprovalRequestRequest request) {

        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Asset not found"));

        User requester = userRepository.findById(request.getRequesterId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        ApprovalWorkflow workflow = approvalWorkflowRepository
                .findById(request.getWorkflowId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Workflow not found"));

        if (!workflow.isActive()) {
            throw new IllegalStateException(
                    "Workflow is not active"
            );
        }

        if (asset.getStatus() != AssetStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "Asset is not available for assignment"
            );
        }

        ApprovalStep firstStep =
                workflowEngineService.findFirstApplicableStep(
                        asset,
                        workflow
                );

        if (firstStep == null) {
            throw new IllegalStateException(
                    "No applicable approval step found"
            );
        }

        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .asset(asset)
                .requester(requester)
                .workflow(workflow)
                .currentStepOrder(firstStep.getStepOrder())
                .status(ApprovalRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return approvalRequestRepository.save(approvalRequest);
    }
}