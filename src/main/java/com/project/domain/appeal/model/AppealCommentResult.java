package com.project.domain.appeal.model;

import java.time.LocalDateTime;

public record AppealCommentResult(
        Long commentId, Long appealId, Long authorId, String comment, LocalDateTime createdAt) {}
