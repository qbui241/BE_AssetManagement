package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.ApprovalRequestRequest;
import com.assetmanagement.asset_management.dto.ApprovalRequestResponse;
import com.assetmanagement.asset_management.dto.PageResponse;
import com.assetmanagement.asset_management.entity.*;
import com.assetmanagement.asset_management.enums.ActionType;
import com.assetmanagement.asset_management.enums.ApprovalRequestStatus;
import com.assetmanagement.asset_management.enums.AssetStatus;
import com.assetmanagement.asset_management.enums.AssetTrackingType;
import com.assetmanagement.asset_management.exception.AccessDeniedException;
import com.assetmanagement.asset_management.exception.InvalidStatusTransitionException;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.ApprovalRequestRepository;
import com.assetmanagement.asset_management.repository.ApprovalRequestSpecifications;
import com.assetmanagement.asset_management.repository.AssetRepository;
import com.assetmanagement.asset_management.repository.UserRepository;
import com.assetmanagement.asset_management.security.CustomUserDetails;
import org.springframework.data.domain.Pageable;
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

    // Danh sách yêu cầu trong phạm vi quản lý: ADMIN xem toàn hệ thống, các role
    // còn lại chỉ thấy yêu cầu có tài sản thuộc chi nhánh của mình (cùng nguyên
    // tắc ABAC với AssetService.getAllAssets()).
    @Transactional(readOnly = true)
    public PageResponse<ApprovalRequestResponse> getAllRequests(
            ApprovalRequestStatus status,
            Pageable pageable) {

        User currentUser = getCurrentUser();
        Long branchScope = hasRole(currentUser, ROLE_ADMIN)
                ? null
                : currentUser.getDepartment().getBranch().getId();

        var spec = ApprovalRequestSpecifications.withFilters(status, null, branchScope);

        return PageResponse.from(
                approvalRequestRepository.findAll(spec, pageable).map(this::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ApprovalRequestResponse> getMyRequests(
            ApprovalRequestStatus status,
            Pageable pageable) {

        User currentUser = getCurrentUser();
        var spec = ApprovalRequestSpecifications.withFilters(status, currentUser.getId(), null);

        return PageResponse.from(
                approvalRequestRepository.findAll(spec, pageable).map(this::toResponse)
        );
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public ApprovalRequestResponse getRequestById(Long id) {
        ApprovalRequest request =
                approvalRequestRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Approval request not found"));

        validateCanViewRequest(request);
        return toResponse(request);
    }

    // Dùng chung cho ApprovalTaskService.getTasksByRequestId() - endpoint
    // GET /{requestId}/tasks bị đúng lỗ hổng tương tự getRequestById() (chưa
    // lọc branch), nên tái dùng lại fetch + validateCanViewRequest ở đây thay
    // vì viết lại logic branch-check một lần nữa.
    ApprovalRequest getRequestEntityForView(Long id) {
        ApprovalRequest request =
                approvalRequestRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Approval request not found"));

        validateCanViewRequest(request);
        return request;
    }

    // @PreAuthorize của endpoint chỉ kiểm tra role/isRequester, không kiểm tra
    // branch - khác với getAllRequests() đã lọc branchScope qua Specification.
    // Thiếu bước này thì xem theo ID sẽ "rò" dữ liệu của chi nhánh khác dù
    // danh sách đã lọc đúng. Dùng lại đúng nguyên tắc: ADMIN không giới hạn,
    // requester luôn xem được yêu cầu của chính mình, còn lại phải cùng chi
    // nhánh với asset của yêu cầu.
    void validateCanViewRequest(ApprovalRequest request) {
        User currentUser = getCurrentUser();

        if (hasRole(currentUser, ROLE_ADMIN)) {
            return;
        }

        if (request.getRequester().getId().equals(currentUser.getId())) {
            return;
        }

        Long currentBranchId = currentUser.getDepartment().getBranch().getId();
        Long assetBranchId = request.getAsset().getDepartment().getBranch().getId();

        if (!currentBranchId.equals(assetBranchId)) {
            throw new AccessDeniedException(
                    "Cannot view an approval request from a different branch."
            );
        }
    }

    @Transactional
    public ApprovalRequestResponse createRequest(ApprovalRequestRequest requestDto) {
        Asset asset = assetRepository.findById(requestDto.getAssetId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));

        ActionType actionType = requestDto.getActionType();
        Integer requestedQuantity = resolveRequestedQuantity(asset, requestDto.getRequestedQuantity());

        validateAssetStatusForAction(asset, actionType, requestedQuantity);

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
                .requestedQuantity(requestedQuantity)
                .build();

        ApprovalRequest savedRequest = approvalRequestRepository.save(approvalRequest);
        workflowEngineService.createInitialTasks(savedRequest);
        return toResponse(savedRequest);
    }

    private Integer resolveRequestedQuantity(Asset asset, Integer requested) {
        if (asset.getTrackingType() == AssetTrackingType.INDIVIDUAL) {
            return null;
        }

        if (requested == null || requested <= 0) {
            throw new IllegalStateException(
                    "requestedQuantity must be a positive number for BULK tracked assets"
            );
        }

        return requested;
    }

    private void validateSameBranch(User requester, Asset asset, boolean allowAdminBypass) {
        if (allowAdminBypass && hasRole(requester, ROLE_ADMIN)) {
            return;
        }

        Long requesterBranchId = requester.getDepartment().getBranch().getId();
        Long assetBranchId = asset.getDepartment().getBranch().getId();

        if (!requesterBranchId.equals(assetBranchId)) {
            throw new IllegalStateException(
                    "Cannot request an asset from a different branch. " +
                            "Cross-branch assignment is not allowed."
            );
        }
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(r -> r.getName().equals(roleName));
    }

    private void validateDisposalRequest(Asset asset, User requester) {
        boolean isPrivilegedUser = hasRole(requester, ROLE_ADMIN) || hasRole(requester, ROLE_MANAGER);
        if (!isPrivilegedUser) {
            throw new AccessDeniedException("User does not have the required role.");
        }

        validateSameBranch(requester, asset, true);
    }

    private void validateAssetStatusForAction(
            Asset asset,
            ActionType actionType,
            Integer requestedQuantity) {

        if (asset.getTrackingType() == AssetTrackingType.INDIVIDUAL) {
            if (actionType == ActionType.ASSIGNMENT && asset.getStatus() != AssetStatus.AVAILABLE) {
                throw new InvalidStatusTransitionException("Asset must be AVAILABLE for assignment");
            }

            if (actionType == ActionType.DISPOSAL && asset.getStatus() != AssetStatus.RETURNED) {
                throw new InvalidStatusTransitionException("Asset must be RETURNED for disposal");
            }
            return;
        }

        if (asset.getAvailableQuantity() == null || asset.getAvailableQuantity() < requestedQuantity) {
            throw new InvalidStatusTransitionException(
                    "Not enough available quantity: requested " + requestedQuantity
                            + " but only " + asset.getAvailableQuantity() + " available"
            );
        }
    }

    private ApprovalRequestResponse toResponse(ApprovalRequest request) {
        return ApprovalRequestResponse.builder()
                .id(request.getId())
                .assetId(request.getAsset().getId())
                .assetCode(request.getAsset().getAssetCode())
                .assetName(request.getAsset().getName())
                .workflowId(request.getWorkflow().getId())
                .workflowName(request.getWorkflow().getName())
                .requesterId(request.getRequester().getId())
                .requesterName(request.getRequester().getName())
                .status(request.getStatus())
                .currentStepOrder(request.getCurrentStepOrder())
                .requestedQuantity(request.getRequestedQuantity())
                .createdAt(request.getCreatedAt())
                .completedAt(request.getCompletedAt())
                .build();
    }
}