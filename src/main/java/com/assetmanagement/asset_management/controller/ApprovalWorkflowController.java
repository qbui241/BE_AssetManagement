package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.ApprovalWorkflowRequest;
import com.assetmanagement.asset_management.dto.ApprovalWorkflowResponse;
import com.assetmanagement.asset_management.service.ApprovalWorkflowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/approval-workflows")
public class ApprovalWorkflowController {

    private final ApprovalWorkflowService approvalWorkflowService;

    public ApprovalWorkflowController(
            ApprovalWorkflowService approvalWorkflowService) {
        this.approvalWorkflowService = approvalWorkflowService;
    }

    @GetMapping
    public List<ApprovalWorkflowResponse> getAllWorkflows() {
        return approvalWorkflowService.getAllWorkflows();
    }

    @GetMapping("/{id}")
    public ApprovalWorkflowResponse getWorkflowById(
            @PathVariable Long id) {

        return approvalWorkflowService.getWorkflowById(id);
    }

    @PostMapping
    public ApprovalWorkflowResponse createWorkflow(
            @Valid @RequestBody ApprovalWorkflowRequest request) {

        return approvalWorkflowService.createWorkflow(request);
    }

    @PutMapping("/{id}")
    public ApprovalWorkflowResponse updateWorkflow(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalWorkflowRequest request) {

        return approvalWorkflowService.updateWorkflow(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteWorkflow(@PathVariable Long id) {
        approvalWorkflowService.deleteWorkflow(id);
    }
}
