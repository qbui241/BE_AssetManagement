package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.BranchRequest;
import com.assetmanagement.asset_management.dto.BranchResponse;
import com.assetmanagement.asset_management.entity.Branch;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.BranchRepository;
import com.assetmanagement.asset_management.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BranchService {

    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;

    public BranchService(BranchRepository branchRepository,
                         DepartmentRepository departmentRepository) {
        this.branchRepository = branchRepository;
        this.departmentRepository = departmentRepository;
    }

    public List<BranchResponse> getAllBranches() {
        return branchRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BranchResponse getBranchById(Long id) {
        return toResponse(getBranchEntity(id));
    }

    public BranchResponse createBranch(BranchRequest request) {
        Branch branch = new Branch();
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());

        return toResponse(branchRepository.save(branch));
    }

    public BranchResponse updateBranch(Long id, BranchRequest request) {
        Branch branch = getBranchEntity(id);
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());

        return toResponse(branchRepository.save(branch));
    }

    public void deleteBranch(Long id) {
        Branch branch = getBranchEntity(id);

        if (departmentRepository.existsByBranchId(id)) {
            throw new IllegalStateException(
                    "Cannot delete branch because it has departments"
            );
        }

        branchRepository.delete(branch);
    }

    private Branch getBranchEntity(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
    }

    private BranchResponse toResponse(Branch branch) {
        return new BranchResponse(branch.getId(), branch.getName(), branch.getAddress());
    }
}