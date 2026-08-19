package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.entity.Branch;
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

    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    public Branch getBranchById(Long id) {
        return branchRepository.findById(id).orElseThrow(()->new RuntimeException("No Branch Found"));
    }

    public Branch createBranch(Branch branch) {
        return branchRepository.save(branch);
    }

    public Branch updateBranch(Long id, Branch request) {
        Branch branch = branchRepository.findById(id).orElseThrow(()->new RuntimeException("No Branch Found"));
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        return branchRepository.save(branch);
    }

    public void deleteBranch(Long id) {

        Branch branch = getBranchById(id);

        if (departmentRepository.existsByBranchId(id)) {
            throw new IllegalStateException(
                    "Cannot delete branch because it has departments"
            );
        }

        branchRepository.delete(branch);
    }
}
