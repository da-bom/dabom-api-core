package com.project.common.auth;

public record TokenRefreshResult(String accessToken, String refreshToken, long expiresIn) {}
