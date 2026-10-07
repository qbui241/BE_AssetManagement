package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.BranchRequest;
import com.assetmanagement.asset_management.dto.BranchResponse;
import com.assetmanagement.asset_management.service.BranchService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping
    public List<BranchResponse> getAllBranchs() {
        return branchService.getAllBranches();
    }

    @GetMapping("/{id}")
    public BranchResponse getBranchById(@PathVariable Long id) {
        return branchService.getBranchById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public BranchResponse createBranch(@Valid @RequestBody BranchRequest request) {
        return branchService.createBranch(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public BranchResponse updateBranch(@PathVariable Long id, @Valid @RequestBody BranchRequest request) {
        return branchService.updateBranch(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteBranch(@PathVariable Long id) {
        branchService.deleteBranch(id);
    }
}