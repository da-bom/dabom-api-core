package com.project.global.exception.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CustomerErrorCode implements BaseErrorCode {
    CUSTOMER_SIGN_IN_FAILED(HttpStatus.UNAUTHORIZED, "CUSTOMER_001", "비밀번호가 올바르지 않습니다."),
    CUSTOMER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "ADMIN_002", "전화번호가 올바르지 않습니다");


    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
