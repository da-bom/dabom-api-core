package com.project.domain.appeal.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.project.domain.appeal.enums.AppealStatus;
import com.project.domain.appeal.model.AppealDetailResult;
import com.project.domain.policy.enums.PolicyType;

/** 이의제기 상세 조회 응답 */
public record AppealDetailResponse(
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
        CommentPageResponse comments) {

    /** 상세 조회 결과 응답 변환 */
    public static AppealDetailResponse from(AppealDetailResult result) {
        return new AppealDetailResponse(
                result.appealId(),
                result.policyAssignmentId(),
                result.policyType(),
                result.requesterId(),
                result.requesterName(),
                result.requestReason(),
                result.rejectReason(),
                result.desiredRules(),
                result.status(),
                result.resolvedById(),
                result.resolvedAt(),
                result.createdAt(),
                CommentPageResponse.from(result.comments()));
    }

    /** 댓글 페이지 응답 */
    public record CommentPageResponse(
            List<CommentItemResponse> content, String nextCursor, boolean hasNext) {

        /** 댓글 페이지 응답 변환 */
        public static CommentPageResponse from(AppealDetailResult.CommentPage comments) {
            return new CommentPageResponse(
                    comments.content().stream().map(CommentItemResponse::from).toList(),
                    comments.nextCursor(),
                    comments.hasNext());
        }
    }

    /** 댓글 항목 응답 */
    public record CommentItemResponse(
            Long commentId,
            Long authorId,
            String authorName,
            String comment,
            LocalDateTime createdAt) {

        /** 댓글 항목 응답 변환 */
        public static CommentItemResponse from(AppealDetailResult.CommentItem comment) {
            return new CommentItemResponse(
                    comment.commentId(),
                    comment.authorId(),
                    comment.authorName(),
                    comment.comment(),
                    comment.createdAt());
        }
    }
}
