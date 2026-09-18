package com.assetmanagement.asset_management.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private String relatedEntityType;
    private Long relatedEntityId;
    private boolean isRead;
    private LocalDateTime createdAt;
}