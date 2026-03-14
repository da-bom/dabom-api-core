package com.project.domain.customer.service;

import java.time.DateTimeException;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.customer.dto.request.CustomerSignInRequest;
import com.project.domain.customer.dto.request.CustomerSignUpRequest;
import com.project.domain.customer.dto.response.SignUpResponse;
import com.project.domain.customer.entity.Customer;
import com.project.domain.customer.entity.CustomerQuota;
import com.project.domain.customer.enums.RoleType;
import com.project.domain.customer.model.MyPageInfo;
import com.project.domain.customer.repository.CustomerQuotaRepository;
import com.project.domain.customer.repository.CustomerRepository;
import com.project.domain.family.entity.FamilyMember;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.family.repository.FamilyRepository;
import com.project.domain.policy.enums.PolicyType;
import com.project.domain.policy.repository.PolicyAssignmentRepository;
import com.project.common.auth.JwtTokenUtil;
import com.project.common.auth.PasswordHash;
import com.project.common.auth.SignInResult;
import com.project.common.auth.TokenRefreshResult;
import com.project.common.auth.model.AuthContext;
import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.CustomerErrorCode;
import com.project.common.exception.code.FamilyErrorCode;
import com.project.common.exception.code.GlobalErrorCode;
import com.project.common.exception.code.PolicyErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyRepository familyRepository;
    private final CustomerQuotaRepository customerQuotaRepository;
    private final PolicyAssignmentRepository policyAssignmentRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final ObjectMapper objectMapper;

    @Override
    public SignInResult signIn(CustomerSignInRequest requestDto) {
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

        return new SignInResult(accessToken, refreshToken, role);
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
    @Transactional(readOnly = true)
    public TokenRefreshResult refreshToken(String refreshToken) {
        try {
            Claims claims = jwtTokenUtil.verifyRefreshToken(refreshToken);
            String roleStr = claims.get("role", String.class);
            if (roleStr == null || RoleType.ADMIN.name().equals(roleStr)) {
                throw new ApplicationException(CustomerErrorCode.CUSTOMER_REFRESH_TOKEN_INVALID);
            }

            Long customerId = Long.parseLong(claims.getSubject());

            if (!customerRepository.existsById(customerId)) {
                throw new ApplicationException(CustomerErrorCode.CUSTOMER_REFRESH_TOKEN_INVALID);
            }

            RoleType currentRole = familyMemberRepository.findRoleById(customerId);
            if (currentRole == null) {
                throw new ApplicationException(CustomerErrorCode.CUSTOMER_REFRESH_TOKEN_INVALID);
            }

            return jwtTokenUtil.reissueTokens(customerId, currentRole);
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApplicationException(CustomerErrorCode.CUSTOMER_REFRESH_TOKEN_INVALID);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MyPageInfo getMyPageInfo(AuthContext authContext, int year, int month) {
        LocalDate targetMonth = resolveTargetMonth(year, month);

        String name =
                customerRepository
                        .findById(authContext.customerId())
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                CustomerErrorCode.CUSTOMER_NOT_FOUND))
                        .getName();

        String familyName =
                familyRepository
                        .findById(authContext.familyId())
                        .orElseThrow(
                                () -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND))
                        .getName();

        CustomerQuota customerQuota =
                customerQuotaRepository
                        .findByFamilyIdAndCustomerIdAndCurrentMonthAndDeletedAtIsNull(
                                authContext.familyId(), authContext.customerId(), targetMonth)
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                CustomerErrorCode.CUSTOMER_NOT_FOUND));
        boolean isBlocked = customerQuota.isBlocked();
        String blockReason = customerQuota.getBlockReason();
        Long monthlyLimitBytes = customerQuota.getMonthlyLimitBytes();
        Long monthlyUsedBytes = customerQuota.getMonthlyUsedBytes();

        String rawRules =
                policyAssignmentRepository
                        .findByTargetAndType(
                                authContext.familyId(),
                                authContext.customerId(),
                                PolicyType.TIME_BLOCK)
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                PolicyErrorCode.POLICY_ASSIGNMENT_NOT_FOUND))
                        .getRules();

        JsonNode timeBlock;
        try {
            timeBlock = objectMapper.readTree(rawRules);
        } catch (JsonProcessingException e) {
            throw new ApplicationException(PolicyErrorCode.POLICY_RULES_SERIALIZATION_FAILED);
        }

        return new MyPageInfo(
                name,
                familyName,
                isBlocked,
                blockReason,
                monthlyLimitBytes,
                monthlyUsedBytes,
                timeBlock);
    }

    private LocalDate resolveTargetMonth(int year, int month) {
        try {
            return LocalDate.of(year, month, 1);
        } catch (DateTimeException e) {
            throw new ApplicationException(GlobalErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
