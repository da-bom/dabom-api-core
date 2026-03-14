package com.project.common.auth;

import com.project.domain.customer.enums.RoleType;

public record SignInResult(String accessToken, String refreshToken, RoleType role) {}
