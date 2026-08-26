package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.ApprovalRequestRequest;
import com.assetmanagement.asset_management.dto.ApprovalRequestResponse;
import com.assetmanagement.asset_management.dto.ApprovalTaskResponse;
import com.assetmanagement.asset_management.entity.ApprovalRequest;
import com.assetmanagement.asset_management.entity.ApprovalTask;
import com.assetmanagement.asset_management.service.ApprovalRequestService;
import com.assetmanagement.asset_management.service.ApprovalTaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/approval-requests")
public class ApprovalRequestController {

    private final ApprovalRequestService approvalRequestService;
    private final ApprovalTaskService approvalTaskService;

    public ApprovalRequestController(
            ApprovalRequestService approvalRequestService,
            ApprovalTaskService approvalTaskService) {
        this.approvalRequestService = approvalRequestService;
        this.approvalTaskService = approvalTaskService;
    }

    @GetMapping("/{requestId}/tasks")
    public List<ApprovalTaskResponse> getTasksByRequestId(
            @PathVariable Long requestId) {
        return approvalTaskService.getTasksByRequestId(requestId);
    }

    @GetMapping
    public List<ApprovalRequestResponse> getAllRequests() {
        return approvalRequestService.getAllRequests();
    }

    @GetMapping("/{id}")
    public ApprovalRequestResponse getRequestById(
            @PathVariable Long id) {
        return approvalRequestService.getRequestById(id);
    }

    @PostMapping
    public ApprovalRequest createRequest(
            @Valid @RequestBody ApprovalRequestRequest request) {

        return approvalRequestService.createRequest(request);
    }
}