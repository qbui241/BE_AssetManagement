package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.ApprovalTaskResponse;
import com.assetmanagement.asset_management.dto.PageResponse;
import com.assetmanagement.asset_management.entity.ApprovalRequest;
import com.assetmanagement.asset_management.entity.ApprovalTask;
import com.assetmanagement.asset_management.entity.Role;
import com.assetmanagement.asset_management.entity.User;
import com.assetmanagement.asset_management.enums.ApprovalRequestStatus;
import com.assetmanagement.asset_management.enums.WorkflowType;
import com.assetmanagement.asset_management.exception.AccessDeniedException;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.ApprovalTaskRepository;
import com.assetmanagement.asset_management.repository.UserRepository;
import com.assetmanagement.asset_management.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApprovalTaskService {

    private final ApprovalTaskRepository approvalTaskRepository;
    private final UserRepository userRepository;
    private final WorkflowEngineService workflowEngineService;

    public ApprovalTaskService(
            ApprovalTaskRepository approvalTaskRepository,
            UserRepository userRepository,
            WorkflowEngineService workflowEngineService) {

        this.approvalTaskRepository = approvalTaskRepository;
        this.userRepository = userRepository;
        this.workflowEngineService = workflowEngineService;
    }

    // Danh sách task toàn hệ thống, có phân trang. Chỉ ADMIN được gọi (xem ở
    // controller) nên không cần thu hẹp theo phạm vi.
    @Transactional(readOnly = true)
    public PageResponse<ApprovalTaskResponse> getTasks(
            Long roleId,
            ApprovalRequestStatus status,
            Pageable pageable) {

        Page<ApprovalTask> tasks;

        if (roleId != null && status != null) {
            tasks = approvalTaskRepository.findByRoleIdAndStatus(roleId, status, pageable);
        } else if (roleId != null) {
            tasks = approvalTaskRepository.findByRoleId(roleId, pageable);
        } else if (status != null) {
            tasks = approvalTaskRepository.findByStatus(status, pageable);
        } else {
            tasks = approvalTaskRepository.findAll(pageable);
        }

        return PageResponse.from(tasks.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ApprovalTaskResponse> getMyPendingTasks(Pageable pageable) {
        User approver = getCurrentAuthenticatedUser();

        List<Long> roleIds = approver.getRoles().stream()
                .map(Role::getId)
                .toList();

        // "IN ()" với danh sách rỗng không hợp lệ ở mọi dialect; người không có
        // vai trò nào thì chắc chắn không có task nào để duyệt.
        if (roleIds.isEmpty()) {
            return PageResponse.from(Page.empty(pageable));
        }

        Page<ApprovalTask> tasks = approvalTaskRepository.findPendingForApprover(
                ApprovalRequestStatus.PENDING,
                WorkflowType.SEQUENTIAL,
                approver.getId(),
                roleIds,
                approver.getDepartment().getId(),
                approver.getDepartment().getBranch().getId(),
                pageable
        );

        return PageResponse.from(tasks.map(task -> toResponse(task, true)));
    }

    // Các task của 1 yêu cầu, kèm cờ canDecide theo người đang xem để giao diện
    // biết có nên hiện nút Duyệt/Từ chối hay không.
    @Transactional(readOnly = true)
    public List<ApprovalTaskResponse> getTasksByRequestId(Long requestId) {
        User viewer = getCurrentAuthenticatedUser();

        return approvalTaskRepository
                .findByApprovalRequestIdOrderByStepOrderAsc(requestId)
                .stream()
                .map(task -> toResponse(task, canDecide(task, viewer)))
                .toList();
    }

    @Transactional
    public ApprovalTaskResponse approveTask(Long taskId) {
        ApprovalTask task = getTaskById(taskId);
        User approver = getCurrentAuthenticatedUser();

        validateTask(task);
        validateApprover(task, approver);

        task.setStatus(ApprovalRequestStatus.APPROVED);
        task.setApprovedAt(LocalDateTime.now());
        task.setApprovedBy(approver);

        ApprovalTask savedTask = approvalTaskRepository.save(task);
        workflowEngineService.processTaskDecision(savedTask);

        return toResponse(savedTask);
    }

    @Transactional
    public ApprovalTaskResponse rejectTask(Long taskId) {
        ApprovalTask task = getTaskById(taskId);
        User approver = getCurrentAuthenticatedUser();

        validateTask(task);
        validateApprover(task, approver);

        task.setStatus(ApprovalRequestStatus.REJECTED);
        task.setApprovedAt(LocalDateTime.now());
        task.setApprovedBy(approver);

        ApprovalTask savedTask = approvalTaskRepository.save(task);
        workflowEngineService.processTaskDecision(savedTask);

        return toResponse(savedTask);
    }

    public ApprovalTask getTaskById(Long id) {
        return approvalTaskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval task not found"));
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void validateTask(ApprovalTask task) {
        if (task.getStatus() != ApprovalRequestStatus.PENDING) {
            throw new IllegalStateException("Approval task has already been processed");
        }

        ApprovalRequest request = task.getApprovalRequest();

        if (request.getStatus() != ApprovalRequestStatus.PENDING) {
            throw new IllegalStateException("Approval request has already been processed");
        }

        if (request.getWorkflow().getType() == WorkflowType.SEQUENTIAL) {
            if (!task.getStepOrder().equals(request.getCurrentStepOrder())) {
                throw new IllegalStateException("This is not the current approval step");
            }
        }
    }

    private void validateApprover(ApprovalTask task, User approver) {
        ApprovalRequest request = task.getApprovalRequest();

        if (request.getRequester().getId().equals(approver.getId())) {
            throw new IllegalStateException("Requester cannot approve their own request");
        }

        boolean hasRequiredRole = approver.getRoles().stream()
                .anyMatch(role -> role.getId().equals(task.getRole().getId()));

        if (!hasRequiredRole) {
            throw new AccessDeniedException("User does not have the required role");
        }

        if (task.getDepartment() != null) {
            if (approver.getDepartment() == null ||
                    !approver.getDepartment().getId().equals(task.getDepartment().getId())) {
                throw new AccessDeniedException(
                        "User does not belong to the required department to process this task"
                );
            }
        }

        if (task.getBranch() != null) {
            if (approver.getDepartment() == null ||
                    approver.getDepartment().getBranch() == null ||
                    !approver.getDepartment().getBranch().getId().equals(task.getBranch().getId())) {
                throw new AccessDeniedException(
                        "User does not belong to the required branch to process this task"
                );
            }
        }
    }

    // Dùng lại đúng validateTask + validateApprover thay vì viết lại điều kiện,
    // để nút "Duyệt" hiện ra khi và chỉ khi thao tác duyệt thực sự được chấp nhận.
    private boolean canDecide(ApprovalTask task, User user) {
        try {
            validateTask(task);
            validateApprover(task, user);
            return true;
        } catch (IllegalStateException | AccessDeniedException e) {
            return false;
        }
    }

    private ApprovalTaskResponse toResponse(ApprovalTask task) {
        return toResponse(task, null);
    }

    private ApprovalTaskResponse toResponse(ApprovalTask task, Boolean canDecide) {
        ApprovalRequest request = task.getApprovalRequest();

        return ApprovalTaskResponse.builder()
                .id(task.getId())
                .approvalRequestId(task.getApprovalRequest().getId())
                .roleId(task.getRole().getId())
                .roleName(task.getRole().getName())
                .stepOrder(task.getStepOrder())
                .status(task.getStatus())
                .approvedAt(task.getApprovedAt())
                .approvedById(task.getApprovedBy() != null ? task.getApprovedBy().getId() : null)
                .approvedByName(task.getApprovedBy() != null ? task.getApprovedBy().getName() : null)
                .note(task.getNote())
                .assetCode(request.getAsset().getAssetCode())
                .assetName(request.getAsset().getName())
                .requesterName(request.getRequester().getName())
                .requestCreatedAt(request.getCreatedAt())
                .canDecide(canDecide)
                .build();
    }
}