package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.DepartmentRequest;
import com.assetmanagement.asset_management.entity.Branch;
import com.assetmanagement.asset_management.entity.Department;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.BranchRepository;
import com.assetmanagement.asset_management.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final BranchRepository branchRepository;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            BranchRepository branchRepository) {

        this.departmentRepository = departmentRepository;
        this.branchRepository = branchRepository;
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Department not found"));
    }

    public Department createDepartment(DepartmentRequest request) {

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Branch not found"));

        Department department = new Department();
        department.setName(request.getName());
        department.setBranch(branch);

        return departmentRepository.save(department);
    }

    public Department updateDepartment(
            Long id,
            DepartmentRequest request) {

        Department department = getDepartmentById(id);

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Branch not found"));

        department.setName(request.getName());
        department.setBranch(branch);

        return departmentRepository.save(department);
    }

    public void deleteDepartment(Long id) {

        Department department = getDepartmentById(id);

        departmentRepository.delete(department);
    }
}