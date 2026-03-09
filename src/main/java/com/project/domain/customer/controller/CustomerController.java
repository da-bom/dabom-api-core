package com.project.domain.customer.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.domain.customer.dto.request.CustomerRefreshRequest;
import com.project.domain.customer.dto.request.CustomerSignInRequest;
import com.project.domain.customer.dto.request.CustomerSignUpRequest;
import com.project.domain.customer.dto.response.SignInResponse;
import com.project.domain.customer.dto.response.SignUpResponse;
import com.project.domain.customer.service.SignInService;
import com.project.global.api.response.ApiResponse;
import com.project.global.auth.TokenRefreshResponse;
import com.project.global.auth.TokenRefreshResult;

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

    @PostMapping("/refresh")
    @Operation(summary = "사용자 토큰 갱신", description = "리프레시 토큰으로 새 액세스 토큰을 발급합니다.")
    public ApiResponse<TokenRefreshResponse> refreshToken(
            @Valid @RequestBody CustomerRefreshRequest request) {
        TokenRefreshResult result = signInService.refreshToken(request.refreshToken());
        return ApiResponse.success(
                new TokenRefreshResponse(
                        result.accessToken(), result.refreshToken(), result.expiresIn()));
    }

    @PostMapping("/logout")
    @Operation(summary = "사용자 로그아웃", description = "사용자 로그아웃 처리합니다. (클라이언트 측 토큰 삭제)")
    public ApiResponse<Void> logout() {
        // 서버 측 처리 없음 — 클라이언트가 토큰을 삭제
        return ApiResponse.success(null);
    }
}
