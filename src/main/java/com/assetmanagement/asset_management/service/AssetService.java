package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.AssetAssignmentRequest;
import com.assetmanagement.asset_management.dto.AssetAttributeValueRequest;
import com.assetmanagement.asset_management.dto.AssetAttributeValueResponse;
import com.assetmanagement.asset_management.dto.AssetRequest;
import com.assetmanagement.asset_management.dto.AssetResponse;
import com.assetmanagement.asset_management.entity.*;
import com.assetmanagement.asset_management.enums.AssetStatus;
import com.assetmanagement.asset_management.exception.InvalidStatusTransitionException;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.*;
import com.assetmanagement.asset_management.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssetCategoryRepository assetCategoryRepository;
    private final UserRepository userRepository;
    private final AssetHistoryRepository assetHistoryRepository;
    private final DepartmentRepository departmentRepository;
    private final AttributeDefinitionRepository attributeDefinitionRepository;
    private final AuditLogService auditLogService;

    public AssetService(
            AssetRepository assetRepository,
            AssetCategoryRepository assetCategoryRepository,
            AssetHistoryRepository assetHistoryRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            AttributeDefinitionRepository attributeDefinitionRepository,
            AuditLogService auditLogService) {

        this.assetRepository = assetRepository;
        this.assetCategoryRepository = assetCategoryRepository;
        this.assetHistoryRepository = assetHistoryRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.attributeDefinitionRepository = attributeDefinitionRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> getAllAssets() {
        return assetRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssetResponse getAssetById(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));
        return toResponse(asset);
    }

    @Transactional
    public AssetResponse createAsset(AssetRequest request) {
        AssetCategory category = assetCategoryRepository
                .findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Department department = departmentRepository
                .findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        Asset asset = new Asset();
        asset.setAssetCode(request.getAssetCode());
        asset.setName(request.getName());
        asset.setSerialNumber(request.getSerialNumber());
        asset.setValue(request.getValue());
        asset.setPurchaseDate(request.getPurchaseDate());
        asset.setStatus(AssetStatus.AVAILABLE);
        asset.setCategory(category);
        asset.setDepartment(department);

        applyAttributeValues(asset, category, request.getAttributes());

        Asset savedAsset = assetRepository.save(asset);
        return toResponse(savedAsset);
    }

    @Transactional
    public AssetResponse updateAsset(Long id, AssetRequest request) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));

        AssetCategory category = assetCategoryRepository
                .findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Department department = departmentRepository
                .findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        asset.setAssetCode(request.getAssetCode());
        asset.setName(request.getName());
        asset.setSerialNumber(request.getSerialNumber());
        asset.setValue(request.getValue());
        asset.setPurchaseDate(request.getPurchaseDate());
        asset.setCategory(category);
        asset.setDepartment(department);
        asset.getAttributeValues().clear();
        applyAttributeValues(asset, category, request.getAttributes());

        Asset updatedAsset = assetRepository.save(asset);
        return toResponse(updatedAsset);
    }

    public void deleteAsset(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));
        assetRepository.delete(asset);
    }

    private void updateStatus(
            Asset asset,
            AssetStatus newStatus) {

        if (!isValidTransition(asset.getStatus(), newStatus)) {
            throw new InvalidStatusTransitionException(
                    "Cannot change status from " + asset.getStatus() + " to " + newStatus
            );
        }

        AssetStatus oldStatus = asset.getStatus();
        asset.setStatus(newStatus);
        auditLogService.log(
                "ASSET",
                asset.getId(),
                "STATUS_CHANGED",
                oldStatus.name(),
                newStatus.name(),
                "Tài sản " + asset.getAssetCode() + " chuyển trạng thái từ "
                        + oldStatus + " sang " + newStatus,
                getCurrentUserReference()
        );
    }

    private User getCurrentUserReference() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return null;
        }

        return userRepository.getReferenceById(userDetails.getUser().getId());
    }

    private boolean isValidTransition(
            AssetStatus currentStatus,
            AssetStatus newStatus) {

        return switch (currentStatus) {
            case AVAILABLE ->
                    newStatus == AssetStatus.ASSIGNED
                            || newStatus == AssetStatus.MAINTENANCE;
            case ASSIGNED ->
                    newStatus == AssetStatus.RETURNED;
            case MAINTENANCE ->
                    newStatus == AssetStatus.RETURNED;
            case RETURNED ->
                    newStatus == AssetStatus.AVAILABLE
                            || newStatus == AssetStatus.MAINTENANCE
                            || newStatus == AssetStatus.DISPOSED;
            case DISPOSED ->
                    false;
        };
    }

    @Transactional
    public AssetResponse assignAsset(
            Long assetId,
            AssetAssignmentRequest request) {

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        updateStatus(asset, AssetStatus.ASSIGNED);

        AssetHistory assetHistory = AssetHistory.builder()
                .asset(asset)
                .user(user)
                .assignedAt(LocalDateTime.now())
                .build();

        assetHistoryRepository.save(assetHistory);
        asset.setAssignedTo(user);

        Asset savedAsset = assetRepository.save(asset);
        return toResponse(savedAsset);
    }

    @Transactional
    public AssetResponse returnAsset(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));

        updateStatus(asset, AssetStatus.RETURNED);
        asset.setAssignedTo(null);

        AssetHistory assetHistory =
                assetHistoryRepository
                        .findByAssetIdAndReturnedAtIsNull(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Active asset history not found"));

        assetHistory.setReturnedAt(LocalDateTime.now());
        assetHistoryRepository.save(assetHistory);

        Asset savedAsset = assetRepository.save(asset);
        return toResponse(savedAsset);
    }

    @Transactional
    public AssetResponse maintenanceAsset(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));

        updateStatus(asset, AssetStatus.MAINTENANCE);

        Asset savedAsset = assetRepository.save(asset);
        return toResponse(savedAsset);
    }

    @Transactional
    public AssetResponse makeAvailable(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));

        updateStatus(asset, AssetStatus.AVAILABLE);

        Asset savedAsset = assetRepository.save(asset);
        return toResponse(savedAsset);
    }

    @Transactional
    public AssetResponse disposeAsset(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));

        updateStatus(asset, AssetStatus.DISPOSED);

        Asset savedAsset = assetRepository.save(asset);
        return toResponse(savedAsset);
    }

    // Validate + build danh sách AssetAttributeValue cho asset theo đúng
    // category, rồi gán vào asset.attributeValues (cascade lo việc lưu).
    // 3 điều kiện được check:
    //   1. attributeDefinitionId gửi lên phải thuộc đúng category của asset
    //      (chặn việc gán nhầm thuộc tính của Laptop cho Xe)
    //   2. Value phải parse được đúng theo dataType (NUMBER/DATE/BOOLEAN)
    //   3. Mọi attribute được đánh dấu required phải có giá trị non-blank
    private void applyAttributeValues(
            Asset asset,
            AssetCategory category,
            List<AssetAttributeValueRequest> attributeRequests) {

        List<AttributeDefinition> definitions =
                attributeDefinitionRepository.findByCategoryId(category.getId());

        Map<Long, AttributeDefinition> definitionsById = new HashMap<>();
        for (AttributeDefinition definition : definitions) {
            definitionsById.put(definition.getId(), definition);
        }

        Map<Long, String> providedValues = new HashMap<>();
        if (attributeRequests != null) {
            for (AssetAttributeValueRequest attributeRequest : attributeRequests) {
                Long definitionId = attributeRequest.getAttributeDefinitionId();

                AttributeDefinition definition = definitionsById.get(definitionId);
                if (definition == null) {
                    throw new IllegalStateException(
                            "Attribute definition " + definitionId
                                    + " does not belong to category '" + category.getName() + "'"
                    );
                }

                validateAttributeValue(definition, attributeRequest.getValue());
                providedValues.put(definitionId, attributeRequest.getValue());
            }
        }

        List<String> missingRequired = new ArrayList<>();
        for (AttributeDefinition definition : definitions) {
            String value = providedValues.get(definition.getId());
            if (definition.isRequired() && (value == null || value.isBlank())) {
                missingRequired.add(definition.getLabel());
            }
        }

        if (!missingRequired.isEmpty()) {
            throw new IllegalStateException(
                    "Missing required attribute(s): " + String.join(", ", missingRequired)
            );
        }

        List<AssetAttributeValue> values = new ArrayList<>();
        for (Map.Entry<Long, String> entry : providedValues.entrySet()) {
            AssetAttributeValue attributeValue = new AssetAttributeValue();
            attributeValue.setAsset(asset);
            attributeValue.setAttributeDefinition(definitionsById.get(entry.getKey()));
            attributeValue.setValue(entry.getValue());
            values.add(attributeValue);
        }

        asset.getAttributeValues().addAll(values);
    }

    private void validateAttributeValue(AttributeDefinition definition, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        try {
            switch (definition.getDataType()) {
                case NUMBER -> Double.parseDouble(value);
                case DATE -> LocalDate.parse(value);
                case BOOLEAN -> {
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                        throw new IllegalArgumentException();
                    }
                }
                case STRING -> {}
            }
        } catch (DateTimeParseException | IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Attribute '" + definition.getLabel() + "' expects a "
                            + definition.getDataType() + " value, got: '" + value + "'"
            );
        }
    }

    private AssetAttributeValueResponse toAttributeValueResponse(AssetAttributeValue value) {
        AttributeDefinition definition = value.getAttributeDefinition();
        return AssetAttributeValueResponse.builder()
                .attributeDefinitionId(definition.getId())
                .name(definition.getName())
                .label(definition.getLabel())
                .dataType(definition.getDataType())
                .value(value.getValue())
                .build();
    }

    private AssetResponse toResponse(Asset asset) {
        return AssetResponse.builder()
                .id(asset.getId())
                .assetCode(asset.getAssetCode())
                .name(asset.getName())
                .serialNumber(asset.getSerialNumber())
                .value(asset.getValue())
                .purchaseDate(asset.getPurchaseDate())
                .status(asset.getStatus())
                .categoryId(asset.getCategory().getId())
                .categoryName(asset.getCategory().getName())
                .departmentId(asset.getDepartment().getId())
                .departmentName(asset.getDepartment().getName())
                .assignedToId(asset.getAssignedTo() != null ? asset.getAssignedTo().getId() : null)
                .assignedToName(asset.getAssignedTo() != null ? asset.getAssignedTo().getName() : null)
                .attributes(asset.getAttributeValues().stream()
                        .map(this::toAttributeValueResponse)
                        .toList())
                .build();
    }
}