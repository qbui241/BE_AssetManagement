package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.ApprovalRequest;
import com.assetmanagement.asset_management.enums.ApprovalRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    boolean existsByIdAndRequesterId(
            Long requestId,
            Long requesterId
    );
    List<ApprovalRequest> findByAssetIdAndStatusAndIdNot(
            Long assetId,
            ApprovalRequestStatus status,
            Long currentRequestId
    );

    boolean existsByAssetIdAndStatus(Long assetId, ApprovalRequestStatus status);

    List<ApprovalRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
}