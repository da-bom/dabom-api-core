package com.project.common.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminErrorCode implements BaseErrorCode {
    ADMIN_SIGN_IN_FAILED(HttpStatus.UNAUTHORIZED, "ADMIN_001", "이메일 또는 비밀번호가 올바르지 않습니다."),
    ADMIN_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "ADMIN_002", "관리자 인증 토큰이 유효하지 않습니다."),
    ADMIN_FORBIDDEN(HttpStatus.FORBIDDEN, "ADMIN_003", "관리자만 접근 가능합니다."),
    ADMIN_REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "ADMIN_004", "유효하지 않은 리프레시 토큰입니다."),
    ADMIN_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMIN_005", "관리자 정보를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
