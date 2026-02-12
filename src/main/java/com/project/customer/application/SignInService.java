package com.project.customer.application;

import com.project.global.auth.JwtTokenUtil;
import org.springframework.stereotype.Service;

import com.project.customer.core.Role;
import com.project.customer.infra.entity.CustomerJpaEntity;
import com.project.customer.infra.repository.CustomerJpaEntityRepository;
import com.project.customer.web.dto.request.SignInRequest;
import com.project.customer.web.dto.response.SignInResponse;
import com.project.family.infra.repository.JpaFamilyMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SignInService {

    private final CustomerJpaEntityRepository customerJpaEntityRepository;
    private final JpaFamilyMemberRepository jpaFamilyMemberRepository;

    private final JwtTokenUtil jwtTokenUtil;

    public SignInResponse signIn(SignInRequest requestDto) {
        // 사용자 조회
        CustomerJpaEntity customer =
                customerJpaEntityRepository.findByPhoneNumber(requestDto.phoneNumber());

        if (customer == null) {
            throw new IllegalArgumentException("Invalid phone number");
        }

        customer.validatePassword(requestDto.password());

        Role role = jpaFamilyMemberRepository.findRoleById(customer.getId());

        String accessToken = jwtTokenUtil.createToken(customer.getId(), role);
        String refreshToken = jwtTokenUtil.createRefreshToken(customer.getId(), role);

        return new SignInResponse(accessToken, refreshToken);
    }
}
