package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.entity.Branch;
import com.assetmanagement.asset_management.repository.BranchRepository;
import com.assetmanagement.asset_management.service.BranchService;
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
    public List<Branch> getAllBranchs() {
        return branchService.getAllBranches();
    }

    @GetMapping("/{id}")
    public Branch getBranchById(@PathVariable Long id) {
        return branchService.getBranchById(id);
    }

    @PostMapping
    public Branch createBranch(@RequestBody Branch branch) {
        return branchService.createBranch(branch);
    }

    @PutMapping("/{id}")
    public Branch updateBranch(@PathVariable Long id,@RequestBody Branch branch) {
        return branchService.updateBranch(id, branch);
    }

    @DeleteMapping("/{id}")
    public void deleteBranch(@PathVariable Long id) {
        branchService.deleteBranch(id);
    }
}
