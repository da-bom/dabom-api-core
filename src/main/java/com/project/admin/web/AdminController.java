package com.project.admin.web;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.admin.application.AdminService;
import com.project.admin.web.dto.SignInRequest;
import com.project.customer.web.dto.response.SignInResponse;
import com.project.global.api.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /** 로그인 */
    @GetMapping("/auth/login")
    public ApiResponse<SignInResponse> signIn(@Valid @RequestBody SignInRequest signInRequest) {
        return ApiResponse.success(adminService.signIn(signInRequest.email(), signInRequest.password()));
    }
}
