package com.project.domain.appeal.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AppealCommentRequest(@NotBlank String comment) {}
