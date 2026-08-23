package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.ApprovalTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalTaskRepository
        extends JpaRepository<ApprovalTask, Long> {

    List<ApprovalTask> findByApprovalRequestIdOrderByStepOrderAsc(
            Long approvalRequestId
    );
}