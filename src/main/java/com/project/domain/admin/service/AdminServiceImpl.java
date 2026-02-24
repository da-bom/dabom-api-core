package com.project.domain.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.admin.entity.Admin;
import com.project.domain.admin.repository.AdminRepository;
import com.project.domain.customer.dto.response.SignInResponse;
import com.project.domain.customer.dto.response.SignUpResponse;
import com.project.domain.customer.enums.RoleType;
import com.project.global.auth.JwtTokenUtil;
import com.project.global.auth.PasswordHash;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.AdminErrorCode;
import com.project.global.exception.code.CustomerErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public SignInResponse signIn(String email, String password) {
        Admin admin =
                adminRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                AdminErrorCode.ADMIN_SIGN_IN_FAILED));

        if (!PasswordHash.matches(password, admin.getPasswordHash())) {
            throw new ApplicationException(CustomerErrorCode.CUSTOMER_SIGN_IN_FAILED);
        }

        String accessToken = jwtTokenUtil.createToken(admin.getId(), RoleType.ADMIN);
        String refreshToken = jwtTokenUtil.createRefreshToken(admin.getId(), RoleType.ADMIN);

        return new SignInResponse(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public SignUpResponse signUp(String email, String password) {
        String hashed = PasswordHash.hash(password);

        Admin admin = Admin.builder().name("ADMIN").email(email).passwordHash(hashed).build();

        adminRepository.save(admin);

        return new SignUpResponse(admin.getId());
    }
}
