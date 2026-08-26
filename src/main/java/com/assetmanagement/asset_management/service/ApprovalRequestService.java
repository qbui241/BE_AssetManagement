package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.ApprovalRequestRequest;
import com.assetmanagement.asset_management.dto.ApprovalRequestResponse;
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
import java.util.List;

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

    public List<ApprovalRequestResponse> getAllRequests() {

        return approvalRequestRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ApprovalRequestResponse getRequestById(Long id) {

        ApprovalRequest request =
                approvalRequestRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Approval request not found"
                                ));

        return toResponse(request);
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

    private ApprovalRequestResponse toResponse(
            ApprovalRequest request) {

        return ApprovalRequestResponse.builder()
                .id(request.getId())

                .assetId(request.getAsset().getId())
                .assetCode(request.getAsset().getAssetCode())
                .assetName(request.getAsset().getName())

                .requesterId(request.getRequester().getId())
                .requesterName(request.getRequester().getName())

                .workflowId(request.getWorkflow().getId())
                .workflowName(request.getWorkflow().getName())

                .currentStepOrder(request.getCurrentStepOrder())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .completedAt(request.getCompletedAt())
                .build();
    }
}