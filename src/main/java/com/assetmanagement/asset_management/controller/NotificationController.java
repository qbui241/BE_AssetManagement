package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.NotificationResponse;
import com.assetmanagement.asset_management.dto.PageResponse;
import com.assetmanagement.asset_management.security.CustomUserDetails;
import com.assetmanagement.asset_management.service.NotificationService;
import com.assetmanagement.asset_management.service.SseEmitterRegistry;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterRegistry sseEmitterRegistry;

    public NotificationController(
            NotificationService notificationService,
            SseEmitterRegistry sseEmitterRegistry) {
        this.notificationService = notificationService;
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    // FE mo ket noi nay bang EventSource va nhan su kien "notification" ngay
    // khi co thong bao moi, thay vi phai cho vong poll tiep theo.
    //
    // EventSource khong the tu set header Authorization, nen o day chap nhan
    // JWT truyen qua query param "access_token" (xem JwtAuthenticationFilter) -
    // chi endpoint nay dung fallback do, moi API khac van bat buoc header.
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return sseEmitterRegistry.register(getCurrentUserId());
    }

    @GetMapping
    public PageResponse<NotificationResponse> getMyNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return notificationService.getMyNotifications(getCurrentUserId(), unreadOnly, pageable);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount() {
        return Map.of("unreadCount", notificationService.countUnread(getCurrentUserId()));
    }

    @PatchMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id, getCurrentUserId());
    }

    @PatchMapping("/read-all")
    public void markAllAsRead() {
        notificationService.markAllAsRead(getCurrentUserId());
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUser().getId();
    }
}