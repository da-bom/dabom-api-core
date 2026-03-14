package com.project.common.api.response;

import com.project.common.auth.TokenRefreshResult;

public record TokenRefreshResponse(String accessToken, String refreshToken, long expiresIn) {

    public static TokenRefreshResponse from(TokenRefreshResult result) {
        return new TokenRefreshResponse(
                result.accessToken(), result.refreshToken(), result.expiresIn());
    }
}
