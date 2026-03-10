package com.project.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminRefreshRequest(@NotBlank String refreshToken) {}
