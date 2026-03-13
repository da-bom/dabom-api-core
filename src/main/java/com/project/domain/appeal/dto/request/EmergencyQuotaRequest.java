package com.project.domain.appeal.dto.request;

import jakarta.validation.constraints.NotBlank;

/** 긴급 쿼터 요청 DTO */
public record EmergencyQuotaRequest(@NotBlank String requestReason) {}
