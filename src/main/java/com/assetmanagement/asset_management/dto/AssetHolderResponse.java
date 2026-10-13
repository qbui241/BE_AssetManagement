package com.assetmanagement.asset_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Một người đang giữ tài sản (một lượt mượn chưa trả). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetHolderResponse {
    private Long historyId;
    private Long userId;
    private String userName;
    private String departmentName;
    private Integer quantity;
    private LocalDateTime assignedAt;
}