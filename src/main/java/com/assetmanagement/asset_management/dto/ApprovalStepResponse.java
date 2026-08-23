package com.assetmanagement.asset_management.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ApprovalStepResponse {

    private Long id;

    private Long workflowId;
    private String workflowName;

    private Long roleId;
    private String roleName;

    private Integer stepOrder;

    private BigDecimal minValue;
    private BigDecimal maxValue;
}