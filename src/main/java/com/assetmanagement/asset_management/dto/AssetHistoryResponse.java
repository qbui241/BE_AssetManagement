package com.assetmanagement.asset_management.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssetHistoryResponse {

    private Long id;
    private Long assetId;
    private String serialNumber;
    private String categoryName;
    private String departmentName;
    private String assetName;
    private String assetCode;
    private String userName;
    private String userEmail;
    private LocalDateTime assignedAt;
    private LocalDateTime returnedAt;
    private Integer quantity;
}
