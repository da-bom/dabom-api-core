package com.project.global.event.dto.notification;

public record QuotaUpdatedPayload(
        Long familyId,
        Long userId,
        Long familyRemainingBytes,
        Double familyUsedPercent,
        Long userUsedBytesCurrentMonth)
        implements NotificationPayload {}
