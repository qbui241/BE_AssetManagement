package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.AssetAssignmentRequest;
import com.assetmanagement.asset_management.dto.AssetAttributeValueRequest;
import com.assetmanagement.asset_management.dto.AssetAttributeValueResponse;
import com.assetmanagement.asset_management.dto.AssetRequest;
import com.assetmanagement.asset_management.dto.AssetResponse;
import com.assetmanagement.asset_management.entity.*;
import com.assetmanagement.asset_management.enums.ApprovalRequestStatus;
import com.assetmanagement.asset_management.enums.AssetStatus;
import com.assetmanagement.asset_management.enums.AssetTrackingType;
import com.assetmanagement.asset_management.exception.AccessDeniedException;
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
    private final ApprovalRequestRepository approvalRequestRepository;

    public AssetService(
            AssetRepository assetRepository,
            AssetCategoryRepository assetCategoryRepository,
            AssetHistoryRepository assetHistoryRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            AttributeDefinitionRepository attributeDefinitionRepository,
            AuditLogService auditLogService,
            ApprovalRequestRepository approvalRequestRepository) {

        this.assetRepository = assetRepository;
        this.assetCategoryRepository = assetCategoryRepository;
        this.assetHistoryRepository = assetHistoryRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.attributeDefinitionRepository = attributeDefinitionRepository;
        this.auditLogService = auditLogService;
        this.approvalRequestRepository = approvalRequestRepository;
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> getAllAssets() {
        User currentUser = getCurrentUser();

        List<Asset> assets = hasRole(currentUser, "ADMIN")
                ? assetRepository.findAll()
                : assetRepository.findByDepartment_Branch_Id(
                currentUser.getDepartment().getBranch().getId()
        );

        return assets.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssetResponse getAssetById(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));

        User currentUser = getCurrentUser();
        if (!hasRole(currentUser, "ADMIN")) {
            Long callerBranchId = currentUser.getDepartment().getBranch().getId();
            Long assetBranchId = asset.getDepartment().getBranch().getId();
            if (!callerBranchId.equals(assetBranchId)) {
                throw new AccessDeniedException("Cannot view asset from a different branch");
            }
        }

        return toResponse(asset);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    @Transactional
    public AssetResponse createAsset(AssetRequest request) {
        AssetCategory category = assetCategoryRepository
                .findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Department department = departmentRepository
                .findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        validateTrackingFields(request);
        validateDepartmentAssignment(getCurrentUser(), null, department);

        Asset asset = new Asset();
        asset.setAssetCode(request.getAssetCode());
        asset.setName(request.getName());
        asset.setValue(request.getValue());
        asset.setPurchaseDate(request.getPurchaseDate());
        asset.setStatus(AssetStatus.AVAILABLE);
        asset.setCategory(category);
        asset.setDepartment(department);
        asset.setTrackingType(request.getTrackingType());
        asset.setSupplier(request.getSupplier());

        if (request.getTrackingType() == AssetTrackingType.INDIVIDUAL) {
            asset.setSerialNumber(request.getSerialNumber());
            asset.setQuantity(null);
            asset.setAvailableQuantity(null);
        } else {
            asset.setSerialNumber(null);
            asset.setQuantity(request.getQuantity());
            asset.setAvailableQuantity(request.getQuantity());
        }

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

        validateTrackingFields(request);
        validateDepartmentAssignment(getCurrentUser(), asset.getDepartment(), department);

        boolean departmentChanged = !asset.getDepartment().getId().equals(department.getId());
        if (departmentChanged && approvalRequestRepository.existsByAssetIdAndStatus(
                asset.getId(), ApprovalRequestStatus.PENDING)) {
            throw new IllegalStateException(
                    "Cannot change department: this asset has a pending approval request"
            );
        }

        boolean trackingTypeChanged = asset.getTrackingType() != request.getTrackingType();
        if (trackingTypeChanged && !assetHistoryRepository.findByAssetId(id).isEmpty()) {
            throw new IllegalStateException(
                    "Cannot change trackingType: this asset already has assignment history"
            );
        }

        asset.setAssetCode(request.getAssetCode());
        asset.setName(request.getName());
        asset.setValue(request.getValue());
        asset.setPurchaseDate(request.getPurchaseDate());
        asset.setCategory(category);
        asset.setDepartment(department);
        asset.setTrackingType(request.getTrackingType());
        asset.setSupplier(request.getSupplier());

        if (request.getTrackingType() == AssetTrackingType.INDIVIDUAL) {
            asset.setSerialNumber(request.getSerialNumber());
            asset.setQuantity(null);
            asset.setAvailableQuantity(null);
        } else {
            asset.setSerialNumber(null);

            int currentlyLentOut = (asset.getQuantity() != null && asset.getAvailableQuantity() != null)
                    ? asset.getQuantity() - asset.getAvailableQuantity()
                    : 0;

            if (request.getQuantity() < currentlyLentOut) {
                throw new IllegalStateException(
                        "Cannot reduce quantity below " + currentlyLentOut
                                + " units currently lent out"
                );
            }

            asset.setQuantity(request.getQuantity());
            asset.setAvailableQuantity(request.getQuantity() - currentlyLentOut);
        }

        asset.getAttributeValues().clear();
        assetRepository.saveAndFlush(asset);
        applyAttributeValues(asset, category, request.getAttributes());

        Asset updatedAsset = assetRepository.save(asset);
        return toResponse(updatedAsset);
    }

    private void validateDepartmentAssignment(
            User currentUser,
            Department currentDepartment,
            Department targetDepartment) {

        if (hasRole(currentUser, "ADMIN")) {
            return;
        }

        if (hasRole(currentUser, "DIRECTOR")) {
            Long callerBranchId = currentUser.getDepartment().getBranch().getId();
            boolean targetOk = callerBranchId.equals(targetDepartment.getBranch().getId());
            boolean currentOk = currentDepartment == null
                    || callerBranchId.equals(currentDepartment.getBranch().getId());

            if (!targetOk || !currentOk) {
                throw new AccessDeniedException(
                        "DIRECTOR can only create/update assets within their own branch"
                );
            }
            return;
        }

        Long ownDepartmentId = currentUser.getDepartment().getId();
        boolean targetOk = ownDepartmentId.equals(targetDepartment.getId());
        boolean currentOk = currentDepartment == null
                || ownDepartmentId.equals(currentDepartment.getId());

        if (!targetOk || !currentOk) {
            throw new AccessDeniedException(
                    "MANAGER can only create/update assets within their own department"
            );
        }
    }

    private void validateTrackingFields(AssetRequest request) {
        if (request.getTrackingType() == AssetTrackingType.INDIVIDUAL) {
            if (request.getSerialNumber() == null || request.getSerialNumber().isBlank()) {
                throw new IllegalStateException(
                        "serialNumber is required for INDIVIDUAL tracked assets"
                );
            }
            if (request.getQuantity() != null) {
                throw new IllegalStateException(
                        "quantity must not be set for INDIVIDUAL tracked assets"
                );
            }
        } else {
            if (request.getSerialNumber() != null && !request.getSerialNumber().isBlank()) {
                throw new IllegalStateException(
                        "serialNumber must not be set for BULK tracked assets"
                );
            }
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                throw new IllegalStateException(
                        "quantity must be a positive number for BULK tracked assets"
                );
            }
        }
    }

    private void requireIndividualTracking(Asset asset) {
        if (asset.getTrackingType() == AssetTrackingType.BULK) {
            throw new IllegalStateException(
                    "This action only applies to INDIVIDUAL tracked assets. "
            );
        }
    }

    private void requireBulkTracking(Asset asset) {
        if (asset.getTrackingType() == AssetTrackingType.INDIVIDUAL) {
            throw new IllegalStateException(
                    "This action only applies to BULK tracked assets. "
            );
        }
    }

    @Transactional
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

        requireIndividualTracking(asset);

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        updateStatus(asset, AssetStatus.ASSIGNED);

        AssetHistory assetHistory = AssetHistory.builder()
                .asset(asset)
                .user(user)
                .assignedAt(LocalDateTime.now())
                .quantity(1)
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

        requireIndividualTracking(asset);

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

        requireIndividualTracking(asset);

        updateStatus(asset, AssetStatus.MAINTENANCE);

        Asset savedAsset = assetRepository.save(asset);
        return toResponse(savedAsset);
    }

    @Transactional
    public AssetResponse makeAvailable(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));

        requireIndividualTracking(asset);

        updateStatus(asset, AssetStatus.AVAILABLE);

        Asset savedAsset = assetRepository.save(asset);
        return toResponse(savedAsset);
    }

    @Transactional
    public AssetResponse disposeAsset(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));

        requireIndividualTracking(asset);

        updateStatus(asset, AssetStatus.DISPOSED);

        Asset savedAsset = assetRepository.save(asset);
        return toResponse(savedAsset);
    }

    @Transactional
    public AssetResponse assignQuantity(Long assetId, Long userId, int quantity) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));

        requireBulkTracking(asset);

        if (asset.getAvailableQuantity() == null || asset.getAvailableQuantity() < quantity) {
            throw new InvalidStatusTransitionException(
                    "Not enough available quantity: requested " + quantity
                            + " but only " + asset.getAvailableQuantity() + " available"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        int oldAvailable = asset.getAvailableQuantity();
        asset.setAvailableQuantity(oldAvailable - quantity);

        if (asset.getAvailableQuantity() == 0) {
            asset.setStatus(AssetStatus.ASSIGNED);
        }

        AssetHistory history = AssetHistory.builder()
                .asset(asset)
                .user(user)
                .quantity(quantity)
                .assignedAt(LocalDateTime.now())
                .build();
        assetHistoryRepository.save(history);

        auditLogService.log(
                "ASSET",
                asset.getId(),
                "QUANTITY_ASSIGNED",
                String.valueOf(oldAvailable),
                String.valueOf(asset.getAvailableQuantity()),
                "Cấp phát " + quantity + " đơn vị tài sản " + asset.getAssetCode()
                        + " cho " + user.getName(),
                getCurrentUserReference()
        );

        Asset savedAsset = assetRepository.save(asset);
        return toResponse(savedAsset);
    }

    @Transactional
    public AssetResponse returnQuantity(Long assetId, Long assetHistoryId) {
        AssetHistory history = assetHistoryRepository.findById(assetHistoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset history not found"));

        if (history.getReturnedAt() != null) {
            throw new IllegalStateException("This loan has already been returned");
        }

        Asset asset = history.getAsset();

        if (!asset.getId().equals(assetId)) {
            throw new IllegalStateException(
                    "Asset history " + assetHistoryId + " does not belong to asset " + assetId
            );
        }

        requireBulkTracking(asset);

        int quantity = history.getQuantity() != null ? history.getQuantity() : 1;

        history.setReturnedAt(LocalDateTime.now());
        assetHistoryRepository.save(history);

        int oldAvailable = asset.getAvailableQuantity() != null ? asset.getAvailableQuantity() : 0;
        asset.setAvailableQuantity(oldAvailable + quantity);

        if (asset.getStatus() == AssetStatus.ASSIGNED && asset.getAvailableQuantity() > 0) {
            asset.setStatus(AssetStatus.AVAILABLE);
        }

        auditLogService.log(
                "ASSET",
                asset.getId(),
                "QUANTITY_RETURNED",
                String.valueOf(oldAvailable),
                String.valueOf(asset.getAvailableQuantity()),
                "Trả lại " + quantity + " đơn vị tài sản " + asset.getAssetCode()
                        + " (lượt mượn #" + assetHistoryId + ")",
                getCurrentUserReference()
        );

        Asset savedAsset = assetRepository.save(asset);
        return toResponse(savedAsset);
    }

    @Transactional
    public AssetResponse disposeQuantity(Long assetId, int quantity) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));

        requireBulkTracking(asset);

        if (asset.getAvailableQuantity() == null || asset.getAvailableQuantity() < quantity) {
            throw new InvalidStatusTransitionException(
                    "Not enough available quantity to dispose: requested " + quantity
                            + " but only " + asset.getAvailableQuantity() + " available"
            );
        }

        int oldAvailable = asset.getAvailableQuantity();
        asset.setAvailableQuantity(oldAvailable - quantity);
        asset.setQuantity(asset.getQuantity() - quantity);

        if (asset.getQuantity() == 0) {
            asset.setStatus(AssetStatus.DISPOSED);
        }

        auditLogService.log(
                "ASSET",
                asset.getId(),
                "QUANTITY_DISPOSED",
                String.valueOf(oldAvailable),
                String.valueOf(asset.getAvailableQuantity()),
                "Thanh lý " + quantity + " đơn vị tài sản " + asset.getAssetCode(),
                getCurrentUserReference()
        );

        Asset savedAsset = assetRepository.save(asset);
        return toResponse(savedAsset);
    }

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
                case STRING -> {
                    // Không cần validate thêm.
                }
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
                .trackingType(asset.getTrackingType())
                .quantity(asset.getQuantity())
                .availableQuantity(asset.getAvailableQuantity())
                .value(asset.getValue())
                .supplier(asset.getSupplier())
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
