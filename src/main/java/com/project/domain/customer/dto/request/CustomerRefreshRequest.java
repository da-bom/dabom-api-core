package com.project.domain.customer.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CustomerRefreshRequest(@NotBlank String refreshToken) {}
