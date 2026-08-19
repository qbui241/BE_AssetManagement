package com.assetmanagement.asset_management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssetAssignmentRequest {

    @NotNull(message = "userId is required")
    private Long userId;
}
