package com.project.domain.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.auth.JwtTokenUtil;
import com.project.common.auth.PasswordHash;
import com.project.common.auth.SignInResult;
import com.project.common.auth.TokenRefreshResult;
import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.AdminErrorCode;
import com.project.domain.admin.entity.Admin;
import com.project.domain.admin.repository.AdminRepository;
import com.project.domain.customer.dto.response.SignUpResponse;
import com.project.domain.customer.enums.RoleType;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public SignInResult signIn(String email, String password) {
        Admin admin =
                adminRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                AdminErrorCode.ADMIN_SIGN_IN_FAILED));

        if (!PasswordHash.matches(password, admin.getPasswordHash())) {
            throw new ApplicationException(AdminErrorCode.ADMIN_SIGN_IN_FAILED);
        }

        String accessToken = jwtTokenUtil.createToken(admin.getId(), RoleType.ADMIN);
        String refreshToken = jwtTokenUtil.createRefreshToken(admin.getId(), RoleType.ADMIN);

        return new SignInResult(accessToken, refreshToken, RoleType.ADMIN);
    }

    @Override
    @Transactional
    public SignUpResponse signUp(String email, String password) {
        String hashed = PasswordHash.hash(password);

        Admin admin = Admin.builder().name("ADMIN").email(email).passwordHash(hashed).build();

        Admin saved = adminRepository.save(admin);

        return new SignUpResponse(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public TokenRefreshResult refreshToken(String refreshToken) {
        try {
            Claims claims = jwtTokenUtil.verifyRefreshToken(refreshToken);
            if (!RoleType.ADMIN.name().equals(claims.get("role", String.class))) {
                throw new ApplicationException(AdminErrorCode.ADMIN_REFRESH_TOKEN_INVALID);
            }

            Long adminId = Long.parseLong(claims.getSubject());

            if (!adminRepository.existsById(adminId)) {
                throw new ApplicationException(AdminErrorCode.ADMIN_REFRESH_TOKEN_INVALID);
            }

            return jwtTokenUtil.reissueTokens(adminId, RoleType.ADMIN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApplicationException(AdminErrorCode.ADMIN_REFRESH_TOKEN_INVALID);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Admin getMe(Long adminId) {
        return adminRepository
                .findById(adminId)
                .orElseThrow(() -> new ApplicationException(AdminErrorCode.ADMIN_NOT_FOUND));
    }
}
