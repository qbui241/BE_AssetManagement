package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.entity.AssetCategory;
import com.assetmanagement.asset_management.service.AssetCategoryService;
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
    public List<AssetCategory> getAllCategories() {
        return assetCategoryService.getAllCategories();
    }

    @GetMapping("/{id}")
    public AssetCategory getCategoryById(@PathVariable Long id) {
        return assetCategoryService.getCategoryById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public AssetCategory createCategory(@RequestBody AssetCategory category) {
        return assetCategoryService.createCategory(category);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public AssetCategory updateCategory(
            @PathVariable Long id,
            @RequestBody AssetCategory category) {

        return assetCategoryService.updateCategory(id, category);
    }

    @PreAuthorize("hasRole('DIRECTOR')")
    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Long id) {
        assetCategoryService.deleteCategory(id);
    }
}