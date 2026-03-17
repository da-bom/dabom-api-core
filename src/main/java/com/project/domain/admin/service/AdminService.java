package com.project.domain.admin.service;

import com.project.common.auth.SignInResult;
import com.project.common.auth.TokenRefreshResult;
import com.project.domain.admin.entity.Admin;
import com.project.domain.customer.dto.response.SignUpResponse;

public interface AdminService {
    SignInResult signIn(String email, String password);

    SignUpResponse signUp(String email, String password);

    TokenRefreshResult refreshToken(String refreshToken);

    Admin getMe(Long adminId);
}
