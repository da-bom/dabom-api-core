package com.project.domain.admin.service;

import com.project.domain.admin.dto.response.AdminRefreshResponse;
import com.project.domain.customer.dto.response.SignInResponse;
import com.project.domain.customer.dto.response.SignUpResponse;

public interface AdminService {
    SignInResponse signIn(String email, String password);

    SignUpResponse signUp(String email, String password);

    AdminRefreshResponse refreshToken(String refreshToken);
}
