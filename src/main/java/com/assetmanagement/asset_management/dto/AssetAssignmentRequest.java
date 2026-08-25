package com.assetmanagement.asset_management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssetAssignmentRequest {

    @NotNull(message = "userId is required")
    private Long userId;
}
