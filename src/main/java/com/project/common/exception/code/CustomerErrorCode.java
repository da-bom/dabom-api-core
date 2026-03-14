package com.project.common.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CustomerErrorCode implements BaseErrorCode {
    CUSTOMER_SIGN_IN_FAILED(HttpStatus.UNAUTHORIZED, "CUSTOMER_001", "비밀번호가 올바르지 않습니다."),
    CUSTOMER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "CUSTOMER_002", "전화번호가 올바르지 않습니다"),
    CUSTOMER_DUPLICATED(HttpStatus.UNAUTHORIZED, "CUSTOMER_003", "이미 존재하는 전화번호입니다"),
    CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND, "CUSTOMER_004", "사용자가 존재하지 않습니다"),
    CUSTOMER_FORBIDDEN(HttpStatus.FORBIDDEN, "CUSTOMER_005", "OWNER 권한이 필요합니다."),
    CUSTOMER_REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "CUSTOMER_006", "유효하지 않은 리프레시 토큰입니다.");

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
