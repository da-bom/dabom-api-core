package com.project.domain.appeal.model;

import java.time.LocalDateTime;

import com.project.domain.appeal.enums.AppealStatus;

public record AppealRespondResult(
        Long appealId,
        AppealStatus status,
        String rejectReason,
        Long resolvedById,
        LocalDateTime resolvedAt) {}
