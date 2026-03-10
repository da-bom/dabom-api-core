package com.project.domain.appeal.dto.request;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 이의제기 생성 요청 DTO */
public record AppealCreateRequest(
        @NotNull @Positive Long policyAssignmentId,
        @NotBlank String requestReason,
        Map<String, Object> desiredRules) {}
