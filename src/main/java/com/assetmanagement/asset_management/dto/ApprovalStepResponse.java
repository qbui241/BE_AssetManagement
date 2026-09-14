package com.assetmanagement.asset_management.dto;

import com.assetmanagement.asset_management.enums.DepartmentScope;
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
    private DepartmentScope departmentScope;
    private Long departmentId;
    private String departmentName;
    private Long branchId;
    private String branchName;
}