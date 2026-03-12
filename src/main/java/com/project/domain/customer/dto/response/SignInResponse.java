package com.project.domain.customer.dto.response;

import com.project.global.auth.SignInResult;

public record SignInResponse(String accessToken, String refreshToken, String role) {

    public static SignInResponse from(SignInResult result) {
        return new SignInResponse(
                result.accessToken(), result.refreshToken(), result.role().name());
    }
}
