package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.ApprovalRequestRequest;
import com.assetmanagement.asset_management.dto.ApprovalRequestResponse;
import com.assetmanagement.asset_management.entity.*;
import com.assetmanagement.asset_management.enums.ActionType;
import com.assetmanagement.asset_management.enums.ApprovalRequestStatus;
import com.assetmanagement.asset_management.enums.AssetStatus;
import com.assetmanagement.asset_management.exception.AccessDeniedException;
import com.assetmanagement.asset_management.exception.InvalidStatusTransitionException;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.ApprovalRequestRepository;
import com.assetmanagement.asset_management.repository.AssetRepository;
import com.assetmanagement.asset_management.repository.UserRepository;
import com.assetmanagement.asset_management.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApprovalRequestService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";

    private final ApprovalRequestRepository approvalRequestRepository;
    private final AssetRepository assetRepository;
    private final WorkflowEngineService workflowEngineService;
    private final UserRepository userRepository;

    public ApprovalRequestService(
            ApprovalRequestRepository approvalRequestRepository,
            AssetRepository assetRepository,
            WorkflowEngineService workflowEngineService,
            UserRepository userRepository) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.assetRepository = assetRepository;
        this.workflowEngineService = workflowEngineService;
        this.userRepository = userRepository;
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
                                new ResourceNotFoundException("Approval request not found"));

        return toResponse(request);
    }

    @Transactional
    public ApprovalRequestResponse createRequest(ApprovalRequestRequest requestDto) {
        Asset asset = assetRepository.findById(requestDto.getAssetId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));

        ActionType actionType = requestDto.getActionType();

        validateAssetStatusForAction(asset, actionType);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User requester = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (actionType == ActionType.ASSIGNMENT) {
            validateSameBranch(requester, asset, false);
        }

        if (actionType == ActionType.DISPOSAL) {
            validateDisposalRequest(asset, requester);
        }

        ApprovalWorkflow workflow = workflowEngineService.findActiveWorkflow(actionType);

        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .asset(asset)
                .workflow(workflow)
                .requester(requester)
                .status(ApprovalRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .currentStepOrder(1)
                .build();

        ApprovalRequest savedRequest = approvalRequestRepository.save(approvalRequest);
        workflowEngineService.createInitialTasks(savedRequest);
        return toResponse(savedRequest);
    }

    private void validateSameBranch(User requester, Asset asset, boolean allowAdminBypass) {
        if (allowAdminBypass && hasRole(requester, ROLE_ADMIN)) {
            return;
        }

        Long requesterBranchId = requester.getDepartment().getBranch().getId();
        Long assetBranchId = asset.getDepartment().getBranch().getId();

        if (!requesterBranchId.equals(assetBranchId)) {
            throw new AccessDeniedException(
                    "Cannot request an asset from a different branch"
            );
        }
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(r -> r.getName().equals(roleName));
    }

    private void validateDisposalRequest(Asset asset, User requester) {
        boolean isPrivilegedUser = hasRole(requester, ROLE_ADMIN) || hasRole(requester, ROLE_MANAGER);
        if (!isPrivilegedUser) {
            throw new AccessDeniedException("User does not have the required role");
        }

        validateSameBranch(requester, asset, true);
    }

    private void validateAssetStatusForAction(Asset asset, ActionType actionType) {
        if (actionType == ActionType.ASSIGNMENT && asset.getStatus() != AssetStatus.AVAILABLE) {
            throw new InvalidStatusTransitionException("Asset must be AVAILABLE for assignment");
        }

        if (actionType == ActionType.DISPOSAL && asset.getStatus() != AssetStatus.RETURNED) {
            throw new InvalidStatusTransitionException("Asset must be RETURNED for disposal");
        }
    }

    private ApprovalRequestResponse toResponse(ApprovalRequest request) {
        return ApprovalRequestResponse.builder()
                .id(request.getId())
                .assetId(request.getAsset().getId())
                .assetName(request.getAsset().getName())
                .workflowId(request.getWorkflow().getId())
                .workflowName(request.getWorkflow().getName())
                .requesterId(request.getRequester().getId())
                .requesterName(request.getRequester().getName())
                .status(request.getStatus())
                .currentStepOrder(request.getCurrentStepOrder())
                .createdAt(request.getCreatedAt())
                .completedAt(request.getCompletedAt())
                .build();
    }
}