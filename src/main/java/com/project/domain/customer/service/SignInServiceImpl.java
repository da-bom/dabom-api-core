package com.project.domain.customer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.customer.dto.request.CustomerSignInRequest;
import com.project.domain.customer.dto.response.SignInResponse;
import com.project.domain.customer.entity.Customer;
import com.project.domain.customer.enums.RoleType;
import com.project.domain.customer.repository.CustomerRepository;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.global.auth.JwtTokenUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SignInServiceImpl implements SignInService {

    private final CustomerRepository customerRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public SignInResponse signIn(CustomerSignInRequest requestDto) {
        Customer customer = customerRepository.findByPhoneNumber(requestDto.phoneNumber());

        if (customer == null) {
            throw new IllegalArgumentException("Invalid phone number");
        }

        customer.validatePassword(requestDto.password());

        RoleType role = familyMemberRepository.findRoleById(customer.getId());

        String accessToken = jwtTokenUtil.createToken(customer.getId(), role);
        String refreshToken = jwtTokenUtil.createRefreshToken(customer.getId(), role);

        return new SignInResponse(accessToken, refreshToken);
    }
}
