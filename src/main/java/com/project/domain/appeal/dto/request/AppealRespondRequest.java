package com.project.domain.appeal.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AppealRespondRequest(@NotBlank String action, String rejectReason) {}
