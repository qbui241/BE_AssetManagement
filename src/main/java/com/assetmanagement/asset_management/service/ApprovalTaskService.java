package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.entity.ApprovalRequest;
import com.assetmanagement.asset_management.entity.ApprovalTask;
import com.assetmanagement.asset_management.entity.User;
import com.assetmanagement.asset_management.enums.ApprovalRequestStatus;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.ApprovalTaskRepository;
import com.assetmanagement.asset_management.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    @Transactional
    public ApprovalTask approveTask(
            Long taskId,
            Long approverId) {

        ApprovalTask task = getTaskById(taskId);

        User approver = userRepository.findById(approverId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));

        validateTask(task);
        validateApprover(task, approver);

        task.setStatus(ApprovalRequestStatus.APPROVED);
        task.setApprovedAt(LocalDateTime.now());
        task.setApprovedBy(approver);

        ApprovalTask savedTask = approvalTaskRepository.save(task);
        workflowEngineService.processApprovedTask(savedTask);
        return savedTask;
    }

    @Transactional
    public ApprovalTask rejectTask(
            Long taskId,
            Long approverId) {

        ApprovalTask task = getTaskById(taskId);

        User approver = userRepository.findById(approverId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));

        validateTask(task);
        validateApprover(task, approver);

        task.setStatus(ApprovalRequestStatus.REJECTED);
        task.setApprovedBy(approver);

        ApprovalTask savedTask = approvalTaskRepository.save(task);

        ApprovalRequest approvalRequest = task.getApprovalRequest();
        approvalRequest.setStatus(ApprovalRequestStatus.REJECTED);
        approvalRequest.setCompletedAt(LocalDateTime.now());

        return savedTask;
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
            throw new IllegalStateException(
                    "User does not have the required role"
            );
        }
    }
}
