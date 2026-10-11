package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository
        extends JpaRepository<Department, Long> {

    boolean existsByBranchId(Long branchId);
    Optional<Department> findByNameAndBranchId(String name, Long branchId);
}