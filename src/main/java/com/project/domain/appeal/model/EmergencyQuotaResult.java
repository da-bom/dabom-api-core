package com.project.domain.appeal.model;

import java.time.LocalDateTime;

import com.project.domain.appeal.enums.AppealStatus;
import com.project.domain.appeal.enums.AppealType;

/** 긴급 쿼터 요청 결과 모델 */
public record EmergencyQuotaResult(
        Long appealId,
        AppealType type,
        AppealStatus status,
        Long additionalBytes,
        Long newMonthlyLimitBytes,
        String requestReason,
        LocalDateTime createdAt) {}
