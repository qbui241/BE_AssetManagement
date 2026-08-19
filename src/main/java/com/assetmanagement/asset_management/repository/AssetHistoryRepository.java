package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.AssetHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetHistoryRepository extends JpaRepository<AssetHistory, Long> {
    List<AssetHistory> findByAssetId(Long assetId);
    List<AssetHistory> findByUserId(Long userId);
    List<AssetHistory> findByAssetIdAndUserId(
            Long assetId,
            Long userId
    );
    Optional<AssetHistory> findByAssetIdAndReturnedAtIsNull(Long assetId);
    boolean existsByUserId(Long userId);
}
