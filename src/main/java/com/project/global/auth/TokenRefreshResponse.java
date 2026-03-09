package com.project.global.auth;

public record TokenRefreshResponse(String accessToken, String refreshToken, long expiresIn) {}
