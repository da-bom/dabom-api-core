package com.project.domain.admin.service;

import com.project.domain.customer.dto.response.SignInResponse;
import com.project.domain.customer.dto.response.SignUpResponse;
import com.project.global.auth.TokenRefreshResult;

public interface AdminService {
    SignInResponse signIn(String email, String password);

    SignUpResponse signUp(String email, String password);

    TokenRefreshResult refreshToken(String refreshToken);
}
