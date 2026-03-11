package com.project.domain.appeal.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.project.domain.appeal.enums.AppealStatus;
import com.project.domain.policy.enums.PolicyType;

/** 이의제기 상세 조회 결과 모델 */
public record AppealDetailResult(
        Long appealId,
        Long policyAssignmentId,
        PolicyType policyType,
        Long requesterId,
        String requesterName,
        String requestReason,
        String rejectReason,
        Map<String, Object> desiredRules,
        AppealStatus status,
        Long resolvedById,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt,
        CommentPage comments) {

    /** 댓글 커서 페이지 결과 모델 */
    public record CommentPage(List<CommentItem> content, String nextCursor, boolean hasNext) {}

    /** 댓글 항목 모델 */
    public record CommentItem(
            Long commentId,
            Long authorId,
            String authorName,
            String comment,
            LocalDateTime createdAt) {}
}
