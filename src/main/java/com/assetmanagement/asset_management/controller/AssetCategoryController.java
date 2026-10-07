package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.AssetCategoryRequest;
import com.assetmanagement.asset_management.dto.AssetCategoryResponse;
import com.assetmanagement.asset_management.service.AssetCategoryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/categories")
public class AssetCategoryController {

    private final AssetCategoryService assetCategoryService;

    public AssetCategoryController(AssetCategoryService assetCategoryService) {
        this.assetCategoryService = assetCategoryService;
    }

    @GetMapping
    public List<AssetCategoryResponse> getAllCategories() {
        return assetCategoryService.getAllCategories();
    }

    @GetMapping("/{id}")
    public AssetCategoryResponse getCategoryById(@PathVariable Long id) {
        return assetCategoryService.getCategoryById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public AssetCategoryResponse createCategory(@Valid @RequestBody AssetCategoryRequest request) {
        return assetCategoryService.createCategory(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public AssetCategoryResponse updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody AssetCategoryRequest request) {

        return assetCategoryService.updateCategory(id, request);
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Long id) {
        assetCategoryService.deleteCategory(id);
    }
}