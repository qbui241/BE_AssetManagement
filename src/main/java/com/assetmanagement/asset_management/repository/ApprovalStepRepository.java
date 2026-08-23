package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.ApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {
    List<ApprovalStep> findByWorkflowIdOrderByStepOrderAsc(Long workflowId);
}
