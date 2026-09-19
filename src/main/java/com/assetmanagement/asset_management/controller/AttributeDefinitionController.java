package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.AttributeDefinitionRequest;
import com.assetmanagement.asset_management.dto.AttributeDefinitionResponse;
import com.assetmanagement.asset_management.service.AttributeDefinitionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Ai đăng nhập cũng đọc được (cần để dựng form tạo/sửa asset động ở
// frontend theo đúng category), nhưng chỉ ADMIN được thêm/sửa/xoá -
// giống nguyên tắc "ADMIN quản lý Workflow, Step, Category, Branch,
// Department" đã áp dụng nhất quán trong toàn hệ thống.
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/attribute-definitions")
public class AttributeDefinitionController {

    private final AttributeDefinitionService attributeDefinitionService;

    public AttributeDefinitionController(AttributeDefinitionService attributeDefinitionService) {
        this.attributeDefinitionService = attributeDefinitionService;
    }

    @GetMapping
    public List<AttributeDefinitionResponse> getAll() {
        return attributeDefinitionService.getAll();
    }

    @GetMapping("/category/{categoryId}")
    public List<AttributeDefinitionResponse> getByCategory(@PathVariable Long categoryId) {
        return attributeDefinitionService.getByCategory(categoryId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AttributeDefinitionResponse create(
            @Valid @RequestBody AttributeDefinitionRequest request) {

        return attributeDefinitionService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AttributeDefinitionResponse update(
            @PathVariable Long id,
            @Valid @RequestBody AttributeDefinitionRequest request) {

        return attributeDefinitionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        attributeDefinitionService.delete(id);
    }
}
