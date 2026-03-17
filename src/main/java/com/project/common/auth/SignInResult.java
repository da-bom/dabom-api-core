package com.project.common.auth;

import com.project.common.auth.enums.RoleType;

public record SignInResult(String accessToken, String refreshToken, RoleType role) {}
