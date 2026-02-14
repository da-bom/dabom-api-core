package com.project.domain.customer.service;

import com.project.domain.customer.dto.request.SignInRequest;
import com.project.domain.customer.dto.response.SignInResponse;

public interface SignInService {
    SignInResponse signIn(SignInRequest requestDto);
}
