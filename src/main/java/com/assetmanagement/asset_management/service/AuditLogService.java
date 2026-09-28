package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.AuditLogResponse;
import com.assetmanagement.asset_management.dto.PageResponse;
import com.assetmanagement.asset_management.entity.AuditLog;
import com.assetmanagement.asset_management.entity.User;
import com.assetmanagement.asset_management.repository.AuditLogRepository;
import com.assetmanagement.asset_management.repository.AuditLogSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(
            String entityType,
            Long entityId,
            String action,
            String oldValue,
            String newValue,
            String description,
            User performedBy) {

        AuditLog log = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .description(description)
                .performedBy(performedBy)
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }

    // Moi: co phan trang + loc theo action/entityType/khoang ngay, tat ca
    // tham so deu optional (truyen null neu khong loc).
    public PageResponse<AuditLogResponse> getLogs(
            String action,
            String entityType,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size) {

        var spec = AuditLogSpecifications.withFilters(action, entityType, from, to);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<AuditLogResponse> result = auditLogRepository.findAll(spec, pageable)
                .map(this::toResponse);

        return PageResponse.from(result);
    }

    public List<AuditLogResponse> getLogsForEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .action(log.getAction())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .description(log.getDescription())
                .performedById(log.getPerformedBy() != null ? log.getPerformedBy().getId() : null)
                .performedByName(log.getPerformedBy() != null ? log.getPerformedBy().getName() : null)
                .createdAt(log.getCreatedAt())
                .build();
    }
}
