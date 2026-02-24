package com.project.domain.customer.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CustomerSignInRequest(
        @NotBlank(message = "전화번호 입력은 필수입니다") String phoneNumber, String password) {}
