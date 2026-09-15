package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.DepartmentRequest;
import com.assetmanagement.asset_management.entity.Department;
import com.assetmanagement.asset_management.service.DepartmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@SecurityRequirement(name = "bearerAuth")
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

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Department createDepartment(
            @RequestBody DepartmentRequest departmentRequest) {

        return departmentService.createDepartment(departmentRequest);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Department updateDepartment(
            @PathVariable Long id,
            @RequestBody DepartmentRequest departmentRequest) {

        return departmentService.updateDepartment(
                id,
                departmentRequest
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteDepartment(
            @PathVariable Long id) {
        departmentService.deleteDepartment(id);
    }
}
