package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.ApprovalTaskRequest;
import com.assetmanagement.asset_management.entity.ApprovalTask;
import com.assetmanagement.asset_management.service.ApprovalTaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/approval-tasks")
public class ApprovalTaskController {

    private final ApprovalTaskService approvalTaskService;

    public ApprovalTaskController(
            ApprovalTaskService approvalTaskService) {
        this.approvalTaskService = approvalTaskService;
    }

    @PostMapping("/{taskId}/approve")
    public ResponseEntity<ApprovalTask> approveTask(
            @PathVariable Long taskId,
            @Valid @RequestBody ApprovalTaskRequest request) {
        ApprovalTask task = approvalTaskService.approveTask(taskId, request.getApproverId());
        return ResponseEntity.ok(task);
    }

    @PostMapping("/{taskId}/reject")
    public ResponseEntity<ApprovalTask> rejectTask(
            @PathVariable Long taskId,
            @Valid @RequestBody ApprovalTaskRequest request) {
        ApprovalTask task = approvalTaskService.rejectTask(taskId, request.getApproverId());
        return ResponseEntity.ok(task);
    }
}

