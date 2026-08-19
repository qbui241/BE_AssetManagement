package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.DepartmentRequest;
import com.assetmanagement.asset_management.entity.Department;
import com.assetmanagement.asset_management.service.DepartmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(
            DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public List<Department> getAllDepartments() {
        return departmentService.getAllDepartments();
    }

    @GetMapping("/{id}")
    public Department getDepartmentById(
            @PathVariable Long id) {

        return departmentService.getDepartmentById(id);
    }

    @PostMapping
    public Department createDepartment(
            @RequestBody DepartmentRequest departmentRequest) {

        return departmentService.createDepartment(departmentRequest);
    }

    @PutMapping("/{id}")
    public Department updateDepartment(
            @PathVariable Long id,
            @RequestBody DepartmentRequest departmentRequest) {

        return departmentService.updateDepartment(
                id,
                departmentRequest
        );
    }

    @DeleteMapping("/{id}")
    public void deleteDepartment(
            @PathVariable Long id) {
        departmentService.deleteDepartment(id);
    }
}
