package com.assetmanagement.asset_management.service;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Giữ các kết nối SSE (Server-Sent Events) đang mở, theo userId, để
 * NotificationService.notify() đẩy thông báo mới xuống ngay khi tạo, thay vì
 * chỉ chờ client tự poll.
 *
 * Một user có thể mở nhiều tab/thiết bị cùng lúc nên value là List, không phải
 * 1 emitter duy nhất.
 */
@Component
public class SseEmitterRegistry {

    // Khong dat vo han: neu client bien mat khong dung cach (rut mang, tat may),
    // timeout giup don emitter chet thay vi ton tai mai trong map. EventSource
    // ben trinh duyet tu dong ket noi lai sau khi bi dong.
    private static final long TIMEOUT_MS = 30 * 60 * 1000L; // 30 phut

    private final Map<Long, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter register(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emittersByUser.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(ex -> remove(userId, emitter));

        return emitter;
    }

    public void sendToUser(Long userId, Object payload) {
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(payload));
            } catch (IOException | IllegalStateException e) {
                // Client da dong ket noi nhung server chua kip nhan biet qua
                // callback onError/onCompletion - don ngay de lan gui sau khong
                // lap lai loi nay.
                remove(userId, emitter);
            }
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByUser.remove(userId);
        }
    }
}
