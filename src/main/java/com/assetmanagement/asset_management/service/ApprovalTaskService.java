package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.ApprovalTaskResponse;
import com.assetmanagement.asset_management.entity.ApprovalRequest;
import com.assetmanagement.asset_management.entity.ApprovalTask;
import com.assetmanagement.asset_management.entity.User;
import com.assetmanagement.asset_management.enums.ApprovalRequestStatus;
import com.assetmanagement.asset_management.exception.AccessDeniedException;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.ApprovalTaskRepository;
import com.assetmanagement.asset_management.repository.UserRepository;
import com.assetmanagement.asset_management.security.CustomUserDetails;
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

    public List<ApprovalTaskResponse> getTasks(
            Long roleId,
            ApprovalRequestStatus status) {

        List<ApprovalTask> tasks;

        if (roleId != null && status != null) {
            tasks = approvalTaskRepository
                    .findByRoleIdAndStatusOrderByStepOrderAsc(
                            roleId,
                            status
                    );
        } else if (roleId != null) {
            tasks = approvalTaskRepository
                    .findByRoleIdOrderByStepOrderAsc(roleId);
        } else if (status != null) {
            tasks = approvalTaskRepository
                    .findByStatusOrderByStepOrderAsc(status);
        } else {
            tasks = approvalTaskRepository
                    .findAll();
        }

        return tasks.stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ApprovalTaskResponse> getTasksByRequestId(Long requestId) {

        return approvalTaskRepository
                .findByApprovalRequestIdOrderByStepOrderAsc(requestId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ApprovalTaskResponse approveTask(Long taskId) {

        ApprovalTask task = getTaskById(taskId);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User approver = userDetails.getUser();

        validateTask(task);
        validateApprover(task, approver);

        task.setStatus(ApprovalRequestStatus.APPROVED);
        task.setApprovedAt(LocalDateTime.now());
        task.setApprovedBy(approver);

        ApprovalTask savedTask = approvalTaskRepository.save(task);
        workflowEngineService.processApprovedTask(savedTask);
        return toResponse(savedTask);
    }

    @Transactional
    public ApprovalTaskResponse rejectTask(
            Long taskId) {

        ApprovalTask task = getTaskById(taskId);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User approver = userDetails.getUser();

        validateTask(task);
        validateApprover(task, approver);

        task.setStatus(ApprovalRequestStatus.REJECTED);
        task.setApprovedBy(approver);

        ApprovalTask savedTask = approvalTaskRepository.save(task);

        ApprovalRequest approvalRequest = task.getApprovalRequest();
        approvalRequest.setStatus(ApprovalRequestStatus.REJECTED);
        approvalRequest.setCompletedAt(LocalDateTime.now());

        return toResponse(savedTask);
    }

    public ApprovalTask getTaskById(Long id) {
        return approvalTaskRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Approval task not found"
                        ));
    }


    private void validateTask(ApprovalTask task) {
        if (task.getStatus() != ApprovalRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Approval task has already been processed"
            );
        }
        ApprovalRequest request = task.getApprovalRequest();

        if (request.getStatus() != ApprovalRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Approval request has already been processed"
            );
        }

        if (!task.getStepOrder().equals(request.getCurrentStepOrder())) {
            throw new IllegalStateException(
                    "This is not the current approval step"
            );
        }
    }

    private void validateApprover(
            ApprovalTask task,
            User approver) {
        ApprovalRequest request = task.getApprovalRequest();

        if (request.getRequester().getId().equals(approver.getId())) {
            throw new IllegalStateException(
                    "Requester cannot approve their own request"
            );
        }

        boolean hasRequiredRole =
                approver.getRoles()
                        .stream()
                        .anyMatch(role -> role.getId().equals(task.getRole().getId())
                        );
        if (!hasRequiredRole) {
            throw new AccessDeniedException(
                    "User does not have the required role"
            );
        }
    }

    private ApprovalTaskResponse toResponse(ApprovalTask task) {

        return ApprovalTaskResponse.builder()
                .id(task.getId())
                .approvalRequestId(
                        task.getApprovalRequest().getId()
                )
                .roleId(
                        task.getRole().getId()
                )
                .roleName(
                        task.getRole().getName()
                )
                .stepOrder(task.getStepOrder())
                .status(task.getStatus())
                .approvedAt(task.getApprovedAt())
                .approvedById(
                        task.getApprovedBy() != null
                                ? task.getApprovedBy().getId()
                                : null
                )
                .approvedByName(
                        task.getApprovedBy() != null
                                ? task.getApprovedBy().getName()
                                : null
                )
                .build();
    }
}
