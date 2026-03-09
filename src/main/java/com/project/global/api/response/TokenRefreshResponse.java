package com.project.global.api.response;

import com.project.global.auth.TokenRefreshResult;

public record TokenRefreshResponse(String accessToken, String refreshToken, long expiresIn) {

    public static TokenRefreshResponse from(TokenRefreshResult result) {
        return new TokenRefreshResponse(
                result.accessToken(), result.refreshToken(), result.expiresIn());
    }
}
