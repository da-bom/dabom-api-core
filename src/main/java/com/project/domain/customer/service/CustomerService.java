package com.project.domain.customer.service;

import com.project.domain.customer.dto.request.CustomerSignInRequest;
import com.project.domain.customer.dto.request.CustomerSignUpRequest;
import com.project.domain.customer.dto.response.SignUpResponse;
import com.project.domain.customer.model.MyPageInfo;
import com.project.global.auth.SignInResult;
import com.project.global.auth.TokenRefreshResult;
import com.project.global.auth.model.AuthContext;

public interface CustomerService {
    SignInResult signIn(CustomerSignInRequest requestDto);

    SignUpResponse signUp(CustomerSignUpRequest requestDto);

    TokenRefreshResult refreshToken(String refreshToken);

    MyPageInfo getMyPageInfo(AuthContext authContext, int year, int month);
}
