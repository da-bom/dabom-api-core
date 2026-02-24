package com.project.domain.customer.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.domain.customer.dto.request.CustomerSignInRequest;
import com.project.domain.customer.dto.request.CustomerSignUpRequest;
import com.project.domain.customer.dto.response.SignInResponse;
import com.project.domain.customer.dto.response.SignUpResponse;
import com.project.domain.customer.service.SignInService;
import com.project.global.api.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/customers")
@Tag(name = "Customers", description = "사용자 인증 API")
@RequiredArgsConstructor
public class CustomerController {

    private final SignInService signInService;

    @PostMapping("/login")
    @Operation(summary = "사용자 로그인", description = "사용자 이메일/비밀번호로 로그인합니다.")
    public ApiResponse<SignInResponse> signIn(
            @Valid @RequestBody CustomerSignInRequest requestDto) {
        return ApiResponse.success(signInService.signIn(requestDto));
    }

    @PostMapping("/signup")
    @Operation(summary = "사용자 회원가입", description = "사용자 이메일/비밀번호로 회원가입합니다.")
    public ApiResponse<SignUpResponse> signUp(
            @Valid @RequestBody CustomerSignUpRequest requestDto) {
        return ApiResponse.success(signInService.signUp(requestDto));
    }
}
