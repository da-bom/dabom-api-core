package com.project.domain.customer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.customer.dto.request.CustomerSignInRequest;
import com.project.domain.customer.dto.request.CustomerSignUpRequest;
import com.project.domain.customer.dto.response.SignInResponse;
import com.project.domain.customer.dto.response.SignUpResponse;
import com.project.domain.customer.entity.Customer;
import com.project.domain.customer.enums.RoleType;
import com.project.domain.customer.repository.CustomerRepository;
import com.project.domain.family.entity.FamilyMember;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.global.auth.JwtTokenUtil;
import com.project.global.auth.PasswordHash;
import com.project.global.auth.TokenRefreshResult;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.CustomerErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SignInServiceImpl implements SignInService {

    private final CustomerRepository customerRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public SignInResponse signIn(CustomerSignInRequest requestDto) {
        Customer customer = customerRepository.findByPhoneNumber(requestDto.phoneNumber());

        if (customer == null) {
            throw new ApplicationException(CustomerErrorCode.CUSTOMER_NOT_FOUND);
        }

        if (!PasswordHash.matches(requestDto.password(), customer.getPasswordHash())) {
            throw new ApplicationException(CustomerErrorCode.CUSTOMER_SIGN_IN_FAILED);
        }

        RoleType role = familyMemberRepository.findRoleById(customer.getId());

        String accessToken = jwtTokenUtil.createToken(customer.getId(), role);
        String refreshToken = jwtTokenUtil.createRefreshToken(customer.getId(), role);

        return new SignInResponse(accessToken, refreshToken);
    }

    @Transactional
    public SignUpResponse signUp(CustomerSignUpRequest requestDto) {
        if (customerRepository.existsByPhoneNumber(requestDto.phoneNumber())) {
            throw new ApplicationException(CustomerErrorCode.CUSTOMER_DUPLICATED);
        }

        String hashed = PasswordHash.hash(requestDto.password());

        Customer customer =
                customerRepository.save(
                        new Customer(requestDto.phoneNumber(), hashed, requestDto.name()));
        FamilyMember familyMember =
                FamilyMember.builder()
                        .familyId(1L)
                        .customerId(customer.getId())
                        .role(RoleType.MEMBER)
                        .build();
        familyMemberRepository.save(familyMember);

        return new SignUpResponse(customer.getId());
    }

    @Override
    public TokenRefreshResult refreshToken(String refreshToken) {
        try {
            Claims claims = jwtTokenUtil.verifyRefreshToken(refreshToken);
            RoleType role = RoleType.valueOf(claims.get("role", String.class));

            if (role == RoleType.ADMIN) {
                throw new ApplicationException(CustomerErrorCode.CUSTOMER_REFRESH_TOKEN_INVALID);
            }

            Long customerId = Long.parseLong(claims.getSubject());
            return jwtTokenUtil.reissueTokens(customerId, role);
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApplicationException(CustomerErrorCode.CUSTOMER_REFRESH_TOKEN_INVALID);
        }
    }
}
