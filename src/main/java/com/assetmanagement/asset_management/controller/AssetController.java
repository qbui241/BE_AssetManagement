package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.AssetAssignmentRequest;
import com.assetmanagement.asset_management.dto.AssetRequest;
import com.assetmanagement.asset_management.dto.AssetResponse;
import com.assetmanagement.asset_management.entity.Asset;
import com.assetmanagement.asset_management.service.AssetService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    public List<AssetResponse> getAllAssets() {
        return assetService.getAllAssets();
    }

    @GetMapping("/{id}")
    public AssetResponse getAssetById(@PathVariable Long id) {
        return assetService.getAssetById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR')")
    public AssetResponse createAsset(
            @Valid @RequestBody AssetRequest request) {

        return assetService.createAsset(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR')")
    public AssetResponse updateAsset(
            @PathVariable Long id,
            @Valid @RequestBody AssetRequest request) {

        return assetService.updateAsset(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public void deleteAsset(@PathVariable Long id) {
        assetService.deleteAsset(id);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR')")
    public AssetResponse assignAsset(
            @PathVariable Long id,
            @Valid @RequestBody AssetAssignmentRequest request) {

        return assetService.assignAsset(id, request);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR')")
    public AssetResponse returnAsset(@PathVariable Long id) {
        return assetService.returnAsset(id);
    }

    @PostMapping("/{id}/maintenance")
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR')")
    public AssetResponse maintenanceAsset(@PathVariable Long id) {
        return assetService.maintenanceAsset(id);
    }

    @PostMapping("/{id}/available")
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR')")
    public AssetResponse makeAvailable(@PathVariable Long id) {
        return assetService.makeAvailable(id);
    }

    @PostMapping("/{id}/dispose")
    @PreAuthorize("hasRole('DIRECTOR')")
    public AssetResponse disposeAsset(@PathVariable Long id) {
        return assetService.disposeAsset(id);
    }
}
