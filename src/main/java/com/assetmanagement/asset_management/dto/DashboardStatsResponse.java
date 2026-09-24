package com.assetmanagement.asset_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private long totalAssetCount;
    private long totalUnits;
    private long availableUnits;
    private BigDecimal totalValue;

    private List<DashboardGroupStat> byCategory;
    private List<DashboardGroupStat> byStatus;
    private List<DashboardGroupStat> byDepartment;
    private List<DashboardGroupStat> byBranch;
    private List<DashboardGroupStat> bySupplier;
}
