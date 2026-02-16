package com.project.domain.admin.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.domain.admin.dto.request.AdminSignInRequest;
import com.project.domain.admin.service.AdminService;
import com.project.domain.customer.dto.response.SignInResponse;
import com.project.global.api.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/auth/login")
    public ApiResponse<SignInResponse> signIn(@Valid @RequestBody AdminSignInRequest signInRequest) {
        return ApiResponse.success(
                adminService.signIn(signInRequest.email(), signInRequest.password()));
    }
}
