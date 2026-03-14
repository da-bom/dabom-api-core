package com.project.domain.appeal.model;

import java.time.LocalDateTime;
import java.util.Map;

import com.project.domain.appeal.enums.AppealStatus;

/** 이의제기 생성 결과 모델 */
public record AppealCreateResult(
        Long appealId,
        Long policyAssignmentId,
        AppealStatus status,
        Boolean policyActive,
        Map<String, Object> desiredRules,
        LocalDateTime createdAt) {}
