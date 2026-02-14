package com.project.domain.customer.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.domain.customer.dto.request.SignInRequest;
import com.project.domain.customer.dto.response.SignInResponse;
import com.project.domain.customer.service.SignInService;
import com.project.global.api.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class CustomerController {

    private final SignInService signInService;

    @GetMapping("/signin")
    public ApiResponse<SignInResponse> signIn(@RequestBody SignInRequest requestDto) {
        return ApiResponse.success(signInService.signIn(requestDto));
    }
}
