package com.assetmanagement.asset_management.dto;

import com.assetmanagement.asset_management.enums.ActionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalRequestRequest {

    @NotNull(message = "assetId is required")
    private Long assetId;

    @NotNull(message = "Action type is required")
    private ActionType actionType;

    private Integer requestedQuantity;
}