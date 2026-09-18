package com.assetmanagement.asset_management.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponse {
    private Long id;
    private String entityType;
    private Long entityId;
    private String action;
    private String oldValue;
    private String newValue;
    private String description;
    private Long performedById;
    private String performedByName;
    private LocalDateTime createdAt;
}