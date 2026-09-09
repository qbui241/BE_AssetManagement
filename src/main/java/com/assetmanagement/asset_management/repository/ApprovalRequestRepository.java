package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    boolean existsByIdAndRequesterId(
            Long requestId,
            Long requesterId
    );
}