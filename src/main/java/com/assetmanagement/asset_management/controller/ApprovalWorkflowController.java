package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.ApprovalWorkflowRequest;
import com.assetmanagement.asset_management.dto.ApprovalWorkflowResponse;
import com.assetmanagement.asset_management.service.ApprovalWorkflowService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/approval-workflows")
public class ApprovalWorkflowController {

    private final ApprovalWorkflowService approvalWorkflowService;

    public ApprovalWorkflowController(
            ApprovalWorkflowService approvalWorkflowService) {
        this.approvalWorkflowService = approvalWorkflowService;
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR', 'ADMIN')")
    @GetMapping
    public List<ApprovalWorkflowResponse> getAllWorkflows() {
        return approvalWorkflowService.getAllWorkflows();
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR', 'ADMIN')")
    @GetMapping("/{id}")
    public ApprovalWorkflowResponse getWorkflowById(
            @PathVariable Long id) {

        return approvalWorkflowService.getWorkflowById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApprovalWorkflowResponse createWorkflow(
            @Valid @RequestBody ApprovalWorkflowRequest request) {

        return approvalWorkflowService.createWorkflow(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApprovalWorkflowResponse updateWorkflow(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalWorkflowRequest request) {

        return approvalWorkflowService.updateWorkflow(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteWorkflow(@PathVariable Long id) {
        approvalWorkflowService.deleteWorkflow(id);
    }
}
