package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.AssetHistoryResponse;
import com.assetmanagement.asset_management.entity.AssetHistory;
import com.assetmanagement.asset_management.repository.AssetHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssetHistoryService {
    private final AssetHistoryRepository assetHistoryRepository;
    private final AssetHistoryResponse assetHistoryResponse;

    AssetHistoryService(AssetHistoryRepository assetHistoryRepository) {
        this.assetHistoryRepository = assetHistoryRepository;
        this.assetHistoryResponse = new AssetHistoryResponse();
    }

    public List<AssetHistoryResponse> getAssetHistories(
            Long assetId,
            Long userId) {

        List<AssetHistory> histories ;
        if (assetId != null && userId != null) {
            histories =  assetHistoryRepository.findByAssetIdAndUserId(assetId, userId);
        } else if (assetId != null) {
            histories = assetHistoryRepository.findByAssetId(assetId);
        }
        else if (userId != null) {
            histories = assetHistoryRepository.findByUserId(userId);
        }
        else {
            histories = assetHistoryRepository.findAll();
        }
        return histories.stream().map(this::toResponse).toList();
    }

    private AssetHistoryResponse toResponse(AssetHistory assetHistory) {
        AssetHistoryResponse assetHistoryResponse = new AssetHistoryResponse();

        assetHistoryResponse.setId(assetHistory.getId());
        assetHistoryResponse.setAssetCode(assetHistory.getAsset().getAssetCode());
        assetHistoryResponse.setAssetName(assetHistory.getAsset().getName());
        assetHistoryResponse.setUserName(assetHistory.getUser().getName());
        assetHistoryResponse.setUserEmail(assetHistory.getUser().getEmail());
        assetHistoryResponse.setAssignedAt(assetHistory.getAssignedAt());
        assetHistoryResponse.setReturnedAt(assetHistory.getReturnedAt());

        return assetHistoryResponse;
    }
}
