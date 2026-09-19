package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.AssetAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetAttributeValueRepository extends JpaRepository<AssetAttributeValue, Long> {

    List<AssetAttributeValue> findByAssetId(Long assetId);

    List<AssetAttributeValue> findByAttributeDefinitionId(Long attributeDefinitionId);
}
