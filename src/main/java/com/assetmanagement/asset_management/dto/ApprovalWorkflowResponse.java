package com.assetmanagement.asset_management.dto;

import com.assetmanagement.asset_management.enums.ActionType;
import com.assetmanagement.asset_management.enums.WorkflowType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApprovalWorkflowResponse {
    private Long id;
    private String name;
    private String description;
    private WorkflowType type;
    private ActionType actionType;
    private boolean active;
}