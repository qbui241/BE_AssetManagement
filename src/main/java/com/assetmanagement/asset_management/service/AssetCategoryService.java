package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.AssetCategoryRequest;
import com.assetmanagement.asset_management.dto.AssetCategoryResponse;
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

    public List<AssetCategoryResponse> getAllCategories() {
        return assetCategoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AssetCategoryResponse getCategoryById(Long id) {
        return toResponse(getCategoryEntity(id));
    }

    public AssetCategoryResponse createCategory(AssetCategoryRequest request) {
        // Entity duoc dung moi (khong truyen id tu client vao), nen khong co
        // chuyen client tu chon id roi Spring Data JPA goi merge() de de len
        // category co san - rui ro that su khi truoc day controller nhan
        // thang @Entity AssetCategory (co setter id cong khai) lam @RequestBody.
        AssetCategory category = new AssetCategory();
        category.setName(request.getName());
        category.setDescription(request.getDescription());

        return toResponse(assetCategoryRepository.save(category));
    }

    public AssetCategoryResponse updateCategory(Long id, AssetCategoryRequest request) {
        AssetCategory existingCategory = getCategoryEntity(id);

        existingCategory.setName(request.getName());
        existingCategory.setDescription(request.getDescription());

        return toResponse(assetCategoryRepository.save(existingCategory));
    }

    public void deleteCategory(Long id) {
        AssetCategory category = getCategoryEntity(id);

        if (assetRepository.existsByCategoryId(id)) {
            throw new IllegalStateException(
                    "Cannot delete category because it is being used by assets"
            );
        }
        assetCategoryRepository.delete(category);
    }

    private AssetCategory getCategoryEntity(Long id) {
        return assetCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private AssetCategoryResponse toResponse(AssetCategory category) {
        return new AssetCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }
}