package com.project.domain.customer.service;

import com.project.domain.customer.dto.request.CustomerSignInRequest;
import com.project.domain.customer.dto.request.CustomerSignUpRequest;
import com.project.domain.customer.dto.response.CustomerRefreshResponse;
import com.project.domain.customer.dto.response.SignInResponse;
import com.project.domain.customer.dto.response.SignUpResponse;

public interface SignInService {
    SignInResponse signIn(CustomerSignInRequest requestDto);

    SignUpResponse signUp(CustomerSignUpRequest requestDto);

    CustomerRefreshResponse refreshToken(String refreshToken);
}
