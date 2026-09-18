package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.AuditLogResponse;
import com.assetmanagement.asset_management.service.AuditLogService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR', 'ADMIN')")
    public List<AuditLogResponse> getAllLogs() {
        return auditLogService.getAllLogs();
    }

    @GetMapping("/entity")
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR', 'ADMIN')")
    public List<AuditLogResponse> getLogsForEntity(
            @RequestParam String entityType,
            @RequestParam Long entityId) {

        return auditLogService.getLogsForEntity(entityType, entityId);
    }
}