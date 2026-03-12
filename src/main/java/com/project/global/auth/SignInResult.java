package com.project.global.auth;

import com.project.domain.customer.enums.RoleType;

public record SignInResult(String accessToken, String refreshToken, RoleType role) {}
