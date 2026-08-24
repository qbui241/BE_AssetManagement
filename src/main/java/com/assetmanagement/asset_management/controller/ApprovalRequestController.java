package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.ApprovalRequestRequest;
import com.assetmanagement.asset_management.entity.ApprovalRequest;
import com.assetmanagement.asset_management.service.ApprovalRequestService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/approval-requests")
public class ApprovalRequestController {

    private final ApprovalRequestService approvalRequestService;

    public ApprovalRequestController(
            ApprovalRequestService approvalRequestService) {
        this.approvalRequestService = approvalRequestService;
    }

    @PostMapping
    public ApprovalRequest createRequest(
            @Valid @RequestBody ApprovalRequestRequest request) {

        return approvalRequestService.createRequest(request);
    }
}