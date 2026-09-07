package com.assetmanagement.asset_management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalRequestRequest {

    @NotNull(message = "assetId is required")
    private Long assetId;
}