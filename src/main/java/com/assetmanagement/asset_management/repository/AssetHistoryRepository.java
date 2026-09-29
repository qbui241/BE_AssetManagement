package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.AssetHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface AssetHistoryRepository
        extends JpaRepository<AssetHistory, Long>, JpaSpecificationExecutor<AssetHistory> {
    List<AssetHistory> findByAssetId(Long assetId);
    Optional<AssetHistory> findByAssetIdAndReturnedAtIsNull(Long assetId);
    boolean existsByUserId(Long userId);
}
