package com.project.customer.application;


import com.project.global.api.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.customer.web.dto.request.SignInRequest;
import com.project.customer.web.dto.response.SignInResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class CustomerController {

    private final SignInService signInService;

    /** 로그인 */
    @GetMapping("/signin")
    public ApiResponse<SignInResponse> signIn(@RequestBody SignInRequest requestDto) {
        return ApiResponse.success(signInService.signIn(requestDto));
    }

}
