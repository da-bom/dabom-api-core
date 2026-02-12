package com.project.admin.application;

import org.springframework.stereotype.Service;

import com.project.admin.application.dto.SignInRequest;
import com.project.admin.infra.entity.AdminJpaEntity;
import com.project.admin.infra.repository.AdminJpaRepository;
import com.project.customer.core.Role;
import com.project.customer.web.dto.response.SignInResponse;
import com.project.global.auth.JwtTokenUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final AdminJpaRepository adminJpaRepository;
    private final JwtTokenUtil jwtTokenUtil;

    public SignInResponse signIn(SignInRequest signInRequest) {
        AdminJpaEntity admin = adminJpaRepository.findByEmail(signInRequest.email());

        if (admin == null) {
            throw new IllegalArgumentException("Invalid email number");
        }

        admin.validatePassword(signInRequest.password());

        String accessToken = jwtTokenUtil.createToken(admin.getId(), Role.ADMIN);
        String refreshToken = jwtTokenUtil.createRefreshToken(admin.getId(), Role.ADMIN);

        return new SignInResponse(accessToken, refreshToken);
    }
}
