package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.AuditLogResponse;
import com.assetmanagement.asset_management.dto.PageResponse;
import com.assetmanagement.asset_management.service.AuditLogService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
    public PageResponse<AuditLogResponse> getLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return auditLogService.getLogs(action, entityType, from, to, page, size);
    }

    @GetMapping("/entity")
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR', 'ADMIN')")
    public List<AuditLogResponse> getLogsForEntity(
            @RequestParam String entityType,
            @RequestParam Long entityId) {

        return auditLogService.getLogsForEntity(entityType, entityId);
    }
}
