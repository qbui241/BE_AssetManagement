package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.AssetHistoryResponse;
import com.assetmanagement.asset_management.entity.AssetHistory;
import com.assetmanagement.asset_management.service.AssetHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/asset-histories")
public class AssetHistoryController {

    private final AssetHistoryService assetHistoryService;

    public AssetHistoryController(AssetHistoryService assetHistoryService) {
        this.assetHistoryService = assetHistoryService;
    }

    @GetMapping
    public List<AssetHistoryResponse> getAssetHistories(
            @RequestParam(required = false) Long assetId,
            @RequestParam(required = false) Long userId) {

        return assetHistoryService.getAssetHistories(assetId, userId);
    }
}

