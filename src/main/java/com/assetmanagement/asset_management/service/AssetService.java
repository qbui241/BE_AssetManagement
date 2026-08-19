package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.AssetAssignmentRequest;
import com.assetmanagement.asset_management.dto.AssetRequest;
import com.assetmanagement.asset_management.dto.AssetResponse;
import com.assetmanagement.asset_management.entity.*;
import com.assetmanagement.asset_management.exception.InvalidStatusTransitionException;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssetCategoryRepository assetCategoryRepository;
    private final UserRepository userRepository;
    private final AssetHistoryRepository assetHistoryRepository;
    private final DepartmentRepository departmentRepository;

    public AssetService(
            AssetRepository assetRepository,
            AssetCategoryRepository assetCategoryRepository,
            AssetHistoryRepository assetHistoryRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository) {

        this.assetRepository = assetRepository;
        this.assetCategoryRepository = assetCategoryRepository;
        this.assetHistoryRepository = assetHistoryRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
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
                .orElseThrow(() ->
                        new ResourceNotFoundException("Asset not found"));

        return toResponse(asset);
    }

    public AssetResponse createAsset(AssetRequest request) {

        AssetCategory category = assetCategoryRepository
                .findById(request.getCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found"));

        Department department = departmentRepository
                .findById(request.getDepartmentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Department not found"));

        Asset asset = new Asset();

        asset.setAssetCode(request.getAssetCode());
        asset.setName(request.getName());
        asset.setSerialNumber(request.getSerialNumber());
        asset.setValue(request.getValue());
        asset.setPurchaseDate(request.getPurchaseDate());
        asset.setStatus(AssetStatus.AVAILABLE);
        asset.setCategory(category);
        asset.setDepartment(department);

        Asset savedAsset = assetRepository.save(asset);

        return toResponse(savedAsset);
    }

    public AssetResponse updateAsset(Long id, AssetRequest request) {

        Asset asset = assetRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Asset not found"));

        AssetCategory category = assetCategoryRepository
                .findById(request.getCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found"));

        Department department = departmentRepository
                .findById(request.getDepartmentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Department not found"));

        asset.setAssetCode(request.getAssetCode());
        asset.setName(request.getName());
        asset.setSerialNumber(request.getSerialNumber());
        asset.setValue(request.getValue());
        asset.setPurchaseDate(request.getPurchaseDate());
        asset.setCategory(category);
        asset.setDepartment(department);

        Asset updatedAsset = assetRepository.save(asset);

        return toResponse(updatedAsset);
    }

    public void deleteAsset(Long id) {

        Asset asset = assetRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Asset not found"));

        assetRepository.delete(asset);
    }

    private void updateStatus(
            Asset asset,
            AssetStatus newStatus) {

        if (!isValidTransition(asset.getStatus(), newStatus)) {

            throw new InvalidStatusTransitionException(
                    "Cannot change status from "
                            + asset.getStatus()
                            + " to "
                            + newStatus
            );
        }

        asset.setStatus(newStatus);
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
                .orElseThrow(() ->
                        new ResourceNotFoundException("Asset not found"));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

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
                .orElseThrow(() ->
                        new ResourceNotFoundException("Asset not found"));

        updateStatus(asset, AssetStatus.RETURNED);

        asset.setAssignedTo(null);

        AssetHistory assetHistory =
                assetHistoryRepository
                        .findByAssetIdAndReturnedAtIsNull(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Active asset history not found"
                                ));

        assetHistory.setReturnedAt(LocalDateTime.now());

        assetHistoryRepository.save(assetHistory);

        Asset savedAsset = assetRepository.save(asset);

        return toResponse(savedAsset);
    }

    @Transactional
    public AssetResponse maintenanceAsset(Long id) {

        Asset asset = assetRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Asset not found"));

        updateStatus(asset, AssetStatus.MAINTENANCE);

        Asset savedAsset = assetRepository.save(asset);

        return toResponse(savedAsset);
    }

    @Transactional
    public AssetResponse makeAvailable(Long id) {

        Asset asset = assetRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Asset not found"));

        updateStatus(asset, AssetStatus.AVAILABLE);

        Asset savedAsset = assetRepository.save(asset);

        return toResponse(savedAsset);
    }

    @Transactional
    public AssetResponse disposeAsset(Long id) {

        Asset asset = assetRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Asset not found"));

        updateStatus(asset, AssetStatus.DISPOSED);

        Asset savedAsset = assetRepository.save(asset);

        return toResponse(savedAsset);
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

                .assignedToId(
                        asset.getAssignedTo() != null
                                ? asset.getAssignedTo().getId()
                                : null
                )
                .assignedToName(
                        asset.getAssignedTo() != null
                                ? asset.getAssignedTo().getName()
                                : null
                )
                .build();
    }
}