package com.project.domain.customer.dto.response;

public record CustomerRefreshResponse(String accessToken, String refreshToken, long expiresIn) {}
