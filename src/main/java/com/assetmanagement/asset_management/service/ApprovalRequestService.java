package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.ApprovalRequestRequest;
import com.assetmanagement.asset_management.entity.*;
import com.assetmanagement.asset_management.enums.ApprovalRequestStatus;
import com.assetmanagement.asset_management.enums.AssetStatus;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.ApprovalRequestRepository;
import com.assetmanagement.asset_management.repository.AssetRepository;
import com.assetmanagement.asset_management.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ApprovalRequestService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final WorkflowEngineService workflowEngineService;


    public ApprovalRequestService(
            ApprovalRequestRepository approvalRequestRepository,
            AssetRepository assetRepository,
            UserRepository userRepository,
            WorkflowEngineService workflowEngineService) {

        this.approvalRequestRepository = approvalRequestRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.workflowEngineService = workflowEngineService;
    }

    @Transactional
    public ApprovalRequest createRequest(
            ApprovalRequestRequest request) {

        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Asset not found"
                        ));

        User requester = userRepository.findById(request.getRequesterId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));

        if (asset.getStatus() != AssetStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "Asset is not available for assignment"
            );
        }

        ApprovalWorkflow workflow =
                workflowEngineService.findActiveWorkflow();

        ApprovalStep firstStep =
                workflowEngineService.findFirstApplicableStep(
                        asset,
                        workflow
                );

        ApprovalRequest approvalRequest =
                ApprovalRequest.builder()
                        .asset(asset)
                        .requester(requester)
                        .workflow(workflow)
                        .currentStepOrder(firstStep.getStepOrder())
                        .status(ApprovalRequestStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build();

        approvalRequest = approvalRequestRepository.save(approvalRequest);
        workflowEngineService.createFirstTask(approvalRequest);
        return approvalRequest;
    }
}