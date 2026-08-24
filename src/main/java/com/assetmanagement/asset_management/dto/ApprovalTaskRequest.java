package com.assetmanagement.asset_management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalTaskRequest {

    @NotNull(message = "Approver ID is required")
    private Long approverId;
}
