package com.project.domain.admin.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.domain.admin.dto.request.AdminSignInRequest;
import com.project.domain.admin.service.AdminService;
import com.project.domain.customer.dto.response.SignInResponse;
import com.project.domain.customer.dto.response.SignUpResponse;
import com.project.global.api.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자 인증 API")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/login")
    @Operation(summary = "관리자 로그인", description = "관리자 이메일/비밀번호로 로그인합니다.")
    public ApiResponse<SignInResponse> signIn(
            @Valid @RequestBody AdminSignInRequest signInRequest) {
        return ApiResponse.success(
                adminService.signIn(signInRequest.email(), signInRequest.password()));
    }

    @PostMapping("/signup")
    @Operation(summary = "관리자 회원가입", description = "관리자 이메일/비밀번호로 회원가입합니다.")
    public ApiResponse<SignUpResponse> signUp(
            @Valid @RequestBody AdminSignInRequest signInRequest) {
        return ApiResponse.success(
                adminService.signUp(signInRequest.email(), signInRequest.password()));
    }
}
