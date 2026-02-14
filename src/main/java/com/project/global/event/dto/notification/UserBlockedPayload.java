package com.project.global.event.dto.notification;

public record UserBlockedPayload(Long familyId, Long userId, String blockReason, String blockedAt)
        implements NotificationPayload {}
