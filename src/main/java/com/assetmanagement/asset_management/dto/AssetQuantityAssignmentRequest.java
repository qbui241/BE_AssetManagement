package com.assetmanagement.asset_management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AssetQuantityAssignmentRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be greater than 0")
    private Integer quantity;
}
