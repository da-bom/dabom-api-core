package com.project.domain.customer.service;

import com.project.domain.customer.dto.request.CustomerSignInRequest;
import com.project.domain.customer.dto.request.CustomerSignUpRequest;
import com.project.domain.customer.dto.response.SignInResponse;
import com.project.domain.customer.dto.response.SignUpResponse;
import com.project.global.auth.TokenRefreshResult;

public interface CustomerService {
    SignInResponse signIn(CustomerSignInRequest requestDto);

    SignUpResponse signUp(CustomerSignUpRequest requestDto);

    TokenRefreshResult refreshToken(String refreshToken);
}
