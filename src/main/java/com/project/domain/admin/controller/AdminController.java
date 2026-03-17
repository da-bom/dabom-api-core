package com.project.domain.admin.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.common.api.response.ApiResponse;
import com.project.common.api.response.TokenRefreshResponse;
import com.project.common.auth.TokenRefreshResult;
import com.project.common.auth.aop.AdminId;
import com.project.common.auth.aop.AdminOnly;
import com.project.domain.admin.dto.request.AdminRefreshRequest;
import com.project.domain.admin.dto.request.AdminSignInRequest;
import com.project.domain.admin.dto.response.AdminDashboardResponse;
import com.project.domain.admin.dto.response.AdminMeResponse;
import com.project.domain.admin.service.AdminDashboardService;
import com.project.domain.admin.service.AdminService;
import com.project.domain.customer.dto.response.SignInResponse;
import com.project.domain.customer.dto.response.SignUpResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자 인증 API")
public class AdminController {

    private final AdminService adminService;
    private final AdminDashboardService adminDashboardService;

    @PostMapping("/login")
    @Operation(summary = "관리자 로그인", description = "관리자 이메일/비밀번호로 로그인합니다.")
    public ApiResponse<SignInResponse> signIn(
            @Valid @RequestBody AdminSignInRequest signInRequest) {
        return ApiResponse.success(
                SignInResponse.from(
                        adminService.signIn(signInRequest.email(), signInRequest.password())));
    }

    @PostMapping("/signup")
    @Operation(summary = "관리자 회원가입", description = "관리자 이메일/비밀번호로 회원가입합니다.")
    public ApiResponse<SignUpResponse> signUp(
            @Valid @RequestBody AdminSignInRequest signInRequest) {
        return ApiResponse.success(
                adminService.signUp(signInRequest.email(), signInRequest.password()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "관리자 토큰 갱신", description = "리프레시 토큰으로 새 액세스 토큰을 발급합니다.")
    public ApiResponse<TokenRefreshResponse> refreshToken(
            @Valid @RequestBody AdminRefreshRequest request) {
        TokenRefreshResult result = adminService.refreshToken(request.refreshToken());
        return ApiResponse.success(TokenRefreshResponse.from(result));
    }

    @GetMapping("/me")
    @AdminOnly
    @Operation(summary = "관리자 내 정보 조회", description = "로그인한 관리자의 기본 프로필 정보를 반환합니다.")
    public ApiResponse<AdminMeResponse> getMe(@Parameter(hidden = true) @AdminId Long adminId) {
        return ApiResponse.success(AdminMeResponse.from(adminService.getMe(adminId)));
    }

    @PostMapping("/logout")
    @Operation(summary = "관리자 로그아웃", description = "관리자 로그아웃 처리합니다. (클라이언트 측 토큰 삭제)")
    public ApiResponse<Void> logout() {
        // 서버 측 처리 없음 — 클라이언트가 토큰을 삭제
        return ApiResponse.success(null);
    }

    @GetMapping("/dashboard")
    @AdminOnly
    @Operation(summary = "관리자 대시보드", description = "시스템 전체 통계를 조회합니다.")
    public ApiResponse<AdminDashboardResponse> getDashboard(
            @Parameter(hidden = true) @AdminId Long adminId) {
        return ApiResponse.success(
                AdminDashboardResponse.from(adminDashboardService.getDashboard()));
    }
}
