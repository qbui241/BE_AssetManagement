package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.ApprovalWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, Long> {
    List<ApprovalWorkflow> findByActiveTrue();
}
