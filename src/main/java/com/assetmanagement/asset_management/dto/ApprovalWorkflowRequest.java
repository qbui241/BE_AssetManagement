package com.assetmanagement.asset_management.dto;

import com.assetmanagement.asset_management.enums.WorkflowType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalWorkflowRequest {

    @NotBlank(message = "Workflow name is required")
    private String name;

    private String description;

    @NotNull(message = "Workflow type is required")
    private WorkflowType type;

    private boolean active;
}