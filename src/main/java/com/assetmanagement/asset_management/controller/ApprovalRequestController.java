package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.ApprovalRequestRequest;
import com.assetmanagement.asset_management.dto.ApprovalRequestResponse;
import com.assetmanagement.asset_management.dto.ApprovalTaskResponse;
import com.assetmanagement.asset_management.service.ApprovalRequestService;
import com.assetmanagement.asset_management.service.ApprovalTaskService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@SecurityRequirement(name = "bearerAuth")
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
    @PreAuthorize(
            "hasAnyRole('MANAGER', 'DIRECTOR') " +
                    "or @approvalRequestSecurity.isRequester(#requestId, authentication)"
    )
    public List<ApprovalTaskResponse> getTasksByRequestId(
            @PathVariable Long requestId) {

        return approvalTaskService.getTasksByRequestId(requestId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR')")
    public List<ApprovalRequestResponse> getAllRequests() {
        return approvalRequestService.getAllRequests();
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public List<ApprovalRequestResponse> getMyRequests() {
        return approvalRequestService.getMyRequests();
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('MANAGER', 'DIRECTOR') " +
                    "or @approvalRequestSecurity.isRequester(#id, authentication)"
    )
    public ApprovalRequestResponse getRequestById(
            @PathVariable Long id) {

        return approvalRequestService.getRequestById(id);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApprovalRequestResponse createRequest(
            @Valid @RequestBody ApprovalRequestRequest request) {

        return approvalRequestService.createRequest(request);
    }
}