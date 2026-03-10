package com.project.global.auth;

public record TokenRefreshResult(String accessToken, String refreshToken, long expiresIn) {}
