package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    boolean existsByCategoryId(Long categoryId);
}