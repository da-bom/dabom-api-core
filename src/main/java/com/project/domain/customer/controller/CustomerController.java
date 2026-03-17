package com.project.domain.customer.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.common.api.response.ApiResponse;
import com.project.common.api.response.TokenRefreshResponse;
import com.project.common.auth.TokenRefreshResult;
import com.project.common.auth.aop.CustomerId;
import com.project.common.auth.model.AuthContext;
import com.project.common.auth.service.AuthContextService;
import com.project.domain.customer.dto.request.CustomerRefreshRequest;
import com.project.domain.customer.dto.request.CustomerSignInRequest;
import com.project.domain.customer.dto.request.CustomerSignUpRequest;
import com.project.domain.customer.dto.response.CustomerMeResponse;
import com.project.domain.customer.dto.response.MyPageInfoResponse;
import com.project.domain.customer.dto.response.SignInResponse;
import com.project.domain.customer.dto.response.SignUpResponse;
import com.project.domain.customer.model.MyPageInfo;
import com.project.domain.customer.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/customers")
@Tag(name = "Customers", description = "사용자 인증 API")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final AuthContextService authContextService;

    @PostMapping("/login")
    @Operation(summary = "사용자 로그인", description = "사용자 이메일/비밀번호로 로그인합니다.")
    public ApiResponse<SignInResponse> signIn(
            @Valid @RequestBody CustomerSignInRequest requestDto) {
        return ApiResponse.success(SignInResponse.from(customerService.signIn(requestDto)));
    }

    @PostMapping("/signup")
    @Operation(summary = "사용자 회원가입", description = "사용자 이메일/비밀번호로 회원가입합니다.")
    public ApiResponse<SignUpResponse> signUp(
            @Valid @RequestBody CustomerSignUpRequest requestDto) {
        return ApiResponse.success(customerService.signUp(requestDto));
    }

    @PostMapping("/refresh")
    @Operation(summary = "사용자 토큰 갱신", description = "리프레시 토큰으로 새 액세스 토큰을 발급합니다.")
    public ApiResponse<TokenRefreshResponse> refreshToken(
            @Valid @RequestBody CustomerRefreshRequest request) {
        TokenRefreshResult result = customerService.refreshToken(request.refreshToken());
        return ApiResponse.success(TokenRefreshResponse.from(result));
    }

    @PostMapping("/logout")
    @Operation(summary = "사용자 로그아웃", description = "사용자 로그아웃 처리합니다. (클라이언트 측 토큰 삭제)")
    public ApiResponse<Void> logout() {
        // 서버 측 처리 없음 — 클라이언트가 토큰을 삭제
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 기본 프로필 정보를 반환합니다.")
    public ApiResponse<CustomerMeResponse> getMe(
            @Parameter(hidden = true) @CustomerId Long customerId) {
        return ApiResponse.success(customerService.getMe(customerId));
    }

    @GetMapping("/mypage")
    @Operation(summary = "마이페이지 조회", description = "본인 이름, 가족 이름, 기본 정책 상세 내용을 반환합니다.")
    public ApiResponse<MyPageInfoResponse> getMyPageInfo(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @RequestParam int year,
            @RequestParam int month) {
        AuthContext authContext = authContextService.resolve(customerId);

        MyPageInfo myPageInfo = customerService.getMyPageInfo(authContext, year, month);

        return ApiResponse.success(MyPageInfoResponse.from(myPageInfo));
    }
}
