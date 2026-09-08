package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.ApprovalTaskRequest;
import com.assetmanagement.asset_management.dto.ApprovalTaskResponse;
import com.assetmanagement.asset_management.entity.ApprovalTask;
import com.assetmanagement.asset_management.enums.ApprovalRequestStatus;
import com.assetmanagement.asset_management.service.ApprovalTaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/approval-tasks")
public class ApprovalTaskController {

    private final ApprovalTaskService approvalTaskService;

    public ApprovalTaskController(
            ApprovalTaskService approvalTaskService) {
        this.approvalTaskService = approvalTaskService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR')")
    public List<ApprovalTaskResponse> getTasks(
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) ApprovalRequestStatus status) {

        return approvalTaskService.getTasks(roleId, status);
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR')")
    @PostMapping("/{taskId}/approve")
    public ResponseEntity<ApprovalTaskResponse> approveTask(
            @PathVariable Long taskId
            ) {
        return ResponseEntity.ok(approvalTaskService.approveTask(taskId));
    }

    @PostMapping("/{taskId}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR')")
    public ResponseEntity<ApprovalTaskResponse> rejectTask(
            @PathVariable Long taskId) {
        return ResponseEntity.ok(approvalTaskService.rejectTask(taskId));
    }
}

