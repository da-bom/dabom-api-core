package com.project.domain.reward.dto.request;

import jakarta.validation.constraints.NotBlank;

/** 보상 요청 승인/거절 처리 요청 DTO다. */
public record RespondRewardRequest(@NotBlank String status, String rejectReason) {}
