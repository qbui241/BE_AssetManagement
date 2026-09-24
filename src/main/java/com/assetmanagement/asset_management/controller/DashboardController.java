package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.DashboardStatsResponse;
import com.assetmanagement.asset_management.service.DashboardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Không giới hạn role cụ thể (mọi user đăng nhập xem được) - dữ liệu đã tự
// scope theo branch ở DashboardService, giống nguyên tắc của GET /api/assets.
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public DashboardStatsResponse getStats() {
        return dashboardService.getStats();
    }
}
