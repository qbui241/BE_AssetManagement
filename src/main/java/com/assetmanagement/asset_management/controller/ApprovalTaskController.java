package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.ApprovalTaskRequest;
import com.assetmanagement.asset_management.dto.ApprovalTaskResponse;
import com.assetmanagement.asset_management.dto.PageResponse;
import com.assetmanagement.asset_management.entity.ApprovalTask;
import com.assetmanagement.asset_management.enums.ApprovalRequestStatus;
import com.assetmanagement.asset_management.service.ApprovalTaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/approval-tasks")
public class ApprovalTaskController {

    private final ApprovalTaskService approvalTaskService;

    public ApprovalTaskController(
            ApprovalTaskService approvalTaskService) {
        this.approvalTaskService = approvalTaskService;
    }

    // Toàn bộ task của hệ thống, không thu hẹp theo phạm vi -> chỉ ADMIN (vai trò
    // không bị giới hạn theo thiết kế). MANAGER/DIRECTOR dùng /mine bên dưới.
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<ApprovalTaskResponse> getTasks(
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) ApprovalRequestStatus status,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        return approvalTaskService.getTasks(roleId, status, pageable);
    }

    // "Chờ tôi duyệt": chỉ các task người gọi đang được phép xử lý. ADMIN cần
    // có mặt ở đây vì escalateTask() có thể đẩy task lên tận ADMIN khi chi
    // nhánh không còn ai ở cấp DIRECTOR - findPendingForApprover() đã coi
    // department/branch NULL của task (trường hợp escalate lên ADMIN) là
    // "không giới hạn", nên không cần sửa gì ở tầng service.
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR', 'ADMIN')")
    public PageResponse<ApprovalTaskResponse> getMyPendingTasks(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        return approvalTaskService.getMyPendingTasks(pageable);
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR', 'ADMIN')")
    @PostMapping("/{taskId}/approve")
    public ResponseEntity<ApprovalTaskResponse> approveTask(
            @PathVariable Long taskId
    ) {
        return ResponseEntity.ok(approvalTaskService.approveTask(taskId));
    }

    @PostMapping("/{taskId}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<ApprovalTaskResponse> rejectTask(
            @PathVariable Long taskId) {
        return ResponseEntity.ok(approvalTaskService.rejectTask(taskId));
    }
}