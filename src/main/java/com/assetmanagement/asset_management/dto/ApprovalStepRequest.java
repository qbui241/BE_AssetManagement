package com.assetmanagement.asset_management.dto;

import com.assetmanagement.asset_management.enums.DepartmentScope;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApprovalStepRequest {

    @NotNull(message = "workflowId is required")
    private Long workflowId;

    @NotNull(message = "roleId is required")
    private Long roleId;

    @NotNull(message = "stepOrder is required")
    private Integer stepOrder;

    private BigDecimal minValue;

    private BigDecimal maxValue;

    @NotNull(message = "departmentScope is required")
    private DepartmentScope departmentScope;

    private Long departmentId; // chi dung khi scope = SPECIFIC_DEPARTMENT
    private Long branchId;     // chi dung khi scope = SPECIFIC_BRANCH
}