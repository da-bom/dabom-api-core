package com.project.domain.admin.dto.response;

public record AdminRefreshResponse(String accessToken, String refreshToken, long expiresIn) {}
