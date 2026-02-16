package com.project.domain.customer.service;

import com.project.domain.customer.dto.request.CustomerSignInRequest;
import com.project.domain.customer.dto.response.SignInResponse;

public interface SignInService {
    SignInResponse signIn(CustomerSignInRequest requestDto);
}
