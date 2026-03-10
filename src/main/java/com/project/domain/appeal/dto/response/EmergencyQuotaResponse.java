package com.project.domain.appeal.dto.response;

import java.time.LocalDateTime;

import com.project.domain.appeal.enums.AppealStatus;
import com.project.domain.appeal.enums.AppealType;
import com.project.domain.appeal.model.EmergencyQuotaResult;

/** 긴급 쿼터 요청 응답 */
public record EmergencyQuotaResponse(
        Long appealId,
        AppealType type,
        AppealStatus status,
        Long additionalBytes,
        Long newMonthlyLimitBytes,
        String requestReason,
        LocalDateTime createdAt) {

    /** 긴급 쿼터 결과 응답 변환 */
    public static EmergencyQuotaResponse from(EmergencyQuotaResult result) {
        return new EmergencyQuotaResponse(
                result.appealId(),
                result.type(),
                result.status(),
                result.additionalBytes(),
                result.newMonthlyLimitBytes(),
                result.requestReason(),
                result.createdAt());
    }
}
