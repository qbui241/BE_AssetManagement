package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType,
            Long entityId
    );

    List<AuditLog> findAllByOrderByCreatedAtDesc();
}