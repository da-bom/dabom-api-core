package com.project.domain.appeal.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.project.domain.appeal.enums.AppealStatus;
import com.project.domain.appeal.enums.AppealType;
import com.project.domain.appeal.model.AppealListResult;

/** 이의제기 목록 조회 응답 */
public record AppealListResponse(
        List<AppealSummaryResponse> appeals, String nextCursor, boolean hasNext) {

    /** 목록 조회 결과 응답 변환 */
    public static AppealListResponse from(AppealListResult result) {
        return new AppealListResponse(
                result.appeals().stream().map(AppealSummaryResponse::from).toList(),
                result.nextCursor(),
                result.hasNext());
    }

    /** 이의제기 목록 항목 응답 */
    public record AppealSummaryResponse(
            Long appealId,
            AppealType type,
            Long policyAssignmentId,
            Long requesterId,
            String requesterName,
            String requestReason,
            Map<String, Object> desiredRules,
            AppealStatus status,
            LocalDateTime createdAt) {

        /** 목록 항목 응답 변환 */
        public static AppealSummaryResponse from(AppealListResult.AppealSummary appeal) {
            return new AppealSummaryResponse(
                    appeal.appealId(),
                    appeal.type(),
                    appeal.policyAssignmentId(),
                    appeal.requesterId(),
                    appeal.requesterName(),
                    appeal.requestReason(),
                    appeal.desiredRules(),
                    appeal.status(),
                    appeal.createdAt());
        }
    }
}
