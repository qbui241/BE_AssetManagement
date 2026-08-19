package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {
}