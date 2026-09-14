package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.ApprovalWorkflow;
import com.assetmanagement.asset_management.enums.ActionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, Long> {
    Optional<ApprovalWorkflow> findByActionTypeAndActiveTrue(ActionType actionType);
}
