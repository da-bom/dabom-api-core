package com.project.domain.appeal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 긴급 쿼터 요청 DTO */
public record EmergencyQuotaRequest(
        @NotBlank String requestReason, @NotNull @Positive Long additionalBytes) {}
