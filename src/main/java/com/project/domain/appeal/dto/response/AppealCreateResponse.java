package com.project.domain.appeal.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

import com.project.domain.appeal.enums.AppealStatus;
import com.project.domain.appeal.model.AppealCreateResult;

/** 이의제기 생성 응답 */
public record AppealCreateResponse(
        Long appealId,
        Long policyAssignmentId,
        AppealStatus status,
        Boolean policyActive,
        Map<String, Object> desiredRules,
        LocalDateTime createdAt) {

    /** 생성 결과 응답 변환 */
    public static AppealCreateResponse from(AppealCreateResult result) {
        return new AppealCreateResponse(
                result.appealId(),
                result.policyAssignmentId(),
                result.status(),
                result.policyActive(),
                result.desiredRules(),
                result.createdAt());
    }
}
