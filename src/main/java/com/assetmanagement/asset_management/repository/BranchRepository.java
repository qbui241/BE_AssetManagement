package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {
}