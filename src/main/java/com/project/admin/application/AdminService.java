package com.project.admin.application;

import org.springframework.stereotype.Service;

import com.project.admin.application.repository.AdminQueryRepository;
import com.project.admin.core.Admin;
import com.project.customer.core.Role;
import com.project.customer.web.dto.response.SignInResponse;
import com.project.global.auth.JwtTokenUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final AdminQueryRepository adminQueryRepository;
    private final JwtTokenUtil jwtTokenUtil;

    public SignInResponse signIn(String email, String password) {
        Admin admin =
                adminQueryRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid email number"));

        admin.validatePassword(password);

        String accessToken = jwtTokenUtil.createToken(admin.getAdminId(), Role.ADMIN);
        String refreshToken = jwtTokenUtil.createRefreshToken(admin.getAdminId(), Role.ADMIN);

        return new SignInResponse(accessToken, refreshToken);
    }
}
