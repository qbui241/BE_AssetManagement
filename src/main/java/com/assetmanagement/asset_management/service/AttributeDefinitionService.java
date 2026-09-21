package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.AttributeDefinitionRequest;
import com.assetmanagement.asset_management.dto.AttributeDefinitionResponse;
import com.assetmanagement.asset_management.entity.AssetAttributeValue;
import com.assetmanagement.asset_management.entity.AssetCategory;
import com.assetmanagement.asset_management.entity.AttributeDefinition;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.AssetAttributeValueRepository;
import com.assetmanagement.asset_management.repository.AssetCategoryRepository;
import com.assetmanagement.asset_management.repository.AttributeDefinitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AttributeDefinitionService {

    private final AttributeDefinitionRepository attributeDefinitionRepository;
    private final AssetCategoryRepository assetCategoryRepository;
    private final AssetAttributeValueRepository assetAttributeValueRepository;

    public AttributeDefinitionService(
            AttributeDefinitionRepository attributeDefinitionRepository,
            AssetCategoryRepository assetCategoryRepository,
            AssetAttributeValueRepository assetAttributeValueRepository) {

        this.attributeDefinitionRepository = attributeDefinitionRepository;
        this.assetCategoryRepository = assetCategoryRepository;
        this.assetAttributeValueRepository = assetAttributeValueRepository;
    }

    @Transactional(readOnly = true)
    public List<AttributeDefinitionResponse> getAll() {
        return attributeDefinitionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttributeDefinitionResponse> getByCategory(Long categoryId) {
        return attributeDefinitionRepository.findByCategoryId(categoryId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AttributeDefinitionResponse create(AttributeDefinitionRequest request) {
        AssetCategory category = assetCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (attributeDefinitionRepository.existsByCategoryIdAndNameIgnoreCase(
                request.getCategoryId(), request.getName())) {
            throw new IllegalStateException(
                    "Attribute '" + request.getName() + "' already exists for this category"
            );
        }

        AttributeDefinition definition = new AttributeDefinition();
        definition.setCategory(category);
        definition.setName(request.getName());
        definition.setLabel(request.getLabel());
        definition.setDataType(request.getDataType());
        definition.setRequired(request.isRequired());

        return toResponse(attributeDefinitionRepository.save(definition));
    }

    @Transactional
    public AttributeDefinitionResponse update(Long id, AttributeDefinitionRequest request) {
        AttributeDefinition definition = attributeDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute definition not found"));

        AssetCategory category = assetCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        boolean nameChanged = !definition.getName().equalsIgnoreCase(request.getName())
                || !definition.getCategory().getId().equals(request.getCategoryId());

        if (nameChanged && attributeDefinitionRepository.existsByCategoryIdAndNameIgnoreCase(
                request.getCategoryId(), request.getName())) {
            throw new IllegalStateException(
                    "Attribute '" + request.getName() + "' already exists for this category"
            );
        }

        // Lưu ý: nếu đổi dataType sau khi đã có asset lưu giá trị theo kiểu
        // cũ, các giá trị cũ sẽ không được validate lại tự động - đây là
        // đánh đổi chấp nhận được ở quy mô đồ án, không tự động migrate data.
        definition.setCategory(category);
        definition.setName(request.getName());
        definition.setLabel(request.getLabel());
        definition.setDataType(request.getDataType());
        definition.setRequired(request.isRequired());

        return toResponse(attributeDefinitionRepository.save(definition));
    }

    @Transactional
    public void delete(Long id) {
        AttributeDefinition definition = attributeDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute definition not found"));
        List<AssetAttributeValue> existingValues =
                assetAttributeValueRepository.findByAttributeDefinitionId(id);
        assetAttributeValueRepository.deleteAll(existingValues);

        attributeDefinitionRepository.delete(definition);
    }

    private AttributeDefinitionResponse toResponse(AttributeDefinition definition) {
        return AttributeDefinitionResponse.builder()
                .id(definition.getId())
                .categoryId(definition.getCategory().getId())
                .categoryName(definition.getCategory().getName())
                .name(definition.getName())
                .label(definition.getLabel())
                .dataType(definition.getDataType())
                .required(definition.isRequired())
                .build();
    }
}
