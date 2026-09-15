package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.ApprovalStepRequest;
import com.assetmanagement.asset_management.dto.ApprovalStepResponse;
import com.assetmanagement.asset_management.entity.ApprovalStep;
import com.assetmanagement.asset_management.service.ApprovalStepService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/approval-steps")
public class ApprovalStepController {

    private final ApprovalStepService approvalStepService;

    public ApprovalStepController(ApprovalStepService approvalStepService) {
        this.approvalStepService = approvalStepService;
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR', 'ADMIN')")
    @GetMapping
    public List<ApprovalStepResponse> getAllSteps() {
        return approvalStepService.getAllSteps();
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR', 'ADMIN')")
    @GetMapping("/{id}")
    public ApprovalStepResponse getStepById(@PathVariable Long id) {
        return approvalStepService.getStepById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApprovalStepResponse createStep(@RequestBody ApprovalStepRequest step) {
        return approvalStepService.createStep(step);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApprovalStepResponse updateStep(
            @PathVariable Long id,
            @RequestBody ApprovalStepRequest step) {
        return approvalStepService.updateStep(id, step);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteStep(@PathVariable Long id) {
        approvalStepService.deleteStep(id);
    }
}