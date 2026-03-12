package com.project.domain.appeal.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.project.domain.appeal.enums.AppealStatus;
import com.project.domain.appeal.enums.AppealType;
import com.project.domain.policy.enums.PolicyType;

/** 이의제기 목록 조회 결과 모델 */
public record AppealListResult(List<AppealSummary> appeals, String nextCursor, boolean hasNext) {

    /** 이의제기 목록 항목 모델 */
    public record AppealSummary(
            Long appealId,
            AppealType type,
            Long policyAssignmentId,
            PolicyType policyType,
            Long requesterId,
            String requesterName,
            String requestReason,
            Map<String, Object> desiredRules,
            AppealStatus status,
            LocalDateTime createdAt) {}
}
