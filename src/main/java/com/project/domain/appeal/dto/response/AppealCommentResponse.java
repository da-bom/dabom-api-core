package com.project.domain.appeal.dto.response;

import java.time.LocalDateTime;

import com.project.domain.appeal.model.AppealCommentResult;

public record AppealCommentResponse(
        Long commentId, Long appealId, Long authorId, String comment, LocalDateTime createdAt) {
    public static AppealCommentResponse from(AppealCommentResult result) {
        return new AppealCommentResponse(
                result.commentId(),
                result.appealId(),
                result.authorId(),
                result.comment(),
                result.createdAt());
    }
}
