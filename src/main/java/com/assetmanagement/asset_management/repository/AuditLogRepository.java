package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType,
            Long entityId
    );

}
