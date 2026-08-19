package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.entity.AssetCategory;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.AssetCategoryRepository;
import com.assetmanagement.asset_management.repository.AssetRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssetCategoryService {

    private final AssetCategoryRepository assetCategoryRepository;
    private final AssetRepository assetRepository;

    public AssetCategoryService(
            AssetCategoryRepository assetCategoryRepository,
            AssetRepository assetRepository) {
        this.assetCategoryRepository = assetCategoryRepository;
        this.assetRepository = assetRepository;
    }

    public List<AssetCategory> getAllCategories() {
        return assetCategoryRepository.findAll();
    }

    public AssetCategory getCategoryById(Long id) {
        return assetCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    public AssetCategory createCategory(AssetCategory category) {
        return assetCategoryRepository.save(category);
    }

    public AssetCategory updateCategory(Long id, AssetCategory category) {
        AssetCategory existingCategory = getCategoryById(id);

        existingCategory.setName(category.getName());
        existingCategory.setDescription(category.getDescription());

        return assetCategoryRepository.save(existingCategory);
    }

    public void deleteCategory(Long id) {
        AssetCategory category = getCategoryById(id);

        if (assetRepository.existsByCategoryId(id)) {
            throw new IllegalStateException(
                    "Cannot delete category because it is being used by assets"
            );
        }
        assetCategoryRepository.delete(category);
    }
}