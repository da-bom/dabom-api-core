package com.project.domain.usagerecord.repository;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UsageSseEmitterRegistry {
    private final Map<Long, List<SseEmitter>> map = new ConcurrentHashMap<>();

    public SseEmitter register(Long familyId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        int id = emitter.hashCode();

        map.computeIfAbsent(familyId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> remove(familyId, emitter);

        emitter.onCompletion(
                () -> {
                    log.info("SSE completed: emitter={}", emitter);
                    cleanup.run();
                });

        emitter.onTimeout(
                () -> {
                    log.info("SSE timed out: emitter={}", emitter);
                    cleanup.run();
                });

        emitter.onError(
                throwable -> {
                    if (isClientDisconnect(throwable)) {
                        log.warn(
                                "SSE client disconnect: emitterId={}, cause={}",
                                id,
                                rootMessage(throwable));
                    } else if (isAlreadyCompleted(throwable)) {
                        log.debug(
                                "SSE already completed: emitterId={}, cause={}",
                                id,
                                rootMessage(throwable));
                    } else {
                        log.error("SSE send failed: emitterId={}, cause={}", id, rootMessage(throwable));
                    }
                    cleanup.run();
                });

        return emitter;
    }

    public void send(Long familyId, String eventName, Object data) {
        List<SseEmitter> list = map.get(familyId);
        if (list == null) {
            return;
        }

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
                log.info("sent success: emitter= id {}", emitter.hashCode());
            } catch (IOException e) {
                remove(familyId, emitter);
            }
        }
    }

    private void remove(Long familyId, SseEmitter emitter) {
        List<SseEmitter> list = map.get(familyId);
        if (list == null) {
            return;
        }
        list.remove(emitter);
        if (list.isEmpty()) {
            map.remove(familyId);
        }
    }

    private boolean isClientDisconnect(Throwable throwable) {
        String message = rootMessage(throwable).toLowerCase();
        return message.contains("broken pipe") || message.contains("clientabort") || message.contains("eof");
    }

    private boolean isAlreadyCompleted(Throwable throwable) {
        return throwable instanceof IllegalStateException;
    }

    private String rootMessage(Throwable throwable) {
        return (throwable.getCause() != null ? throwable.getCause() : throwable).getMessage();
    }
}
