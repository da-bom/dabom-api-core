package com.project.global.event.dto.usage;

public record UsagePersistPayload(
        String originEventId,
        Long familyId,
        Long userId,
        Long bytesUsed,
        String appId,
        String processResult,
        Long remainingAfter,
        String eventTime) {}
