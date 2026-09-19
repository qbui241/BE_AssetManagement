package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.AttributeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, Long> {

    List<AttributeDefinition> findByCategoryId(Long categoryId);

    boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);
}
