package com.project.domain.customer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerSignInRequest(@NotBlank(message = "전화번호 입력은 필수입니다") String phoneNumber,@Size(min=8, message = "최소 8글자 이상 입력해야합니다.") String password) {}
