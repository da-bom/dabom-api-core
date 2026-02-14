package com.project.domain.admin.service;

import com.project.domain.customer.dto.response.SignInResponse;

public interface AdminService {
    SignInResponse signIn(String email, String password);
}
