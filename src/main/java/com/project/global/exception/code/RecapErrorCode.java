package com.project.global.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecapErrorCode implements BaseErrorCode {
    RECAP_NOT_FOUND(HttpStatus.NOT_FOUND, "RECAP_001", "리캡을 찾을 수 없습니다."),
    RECAP_JSON_DESERIALIZATION_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR, "RECAP_002", "리캡 스냅샷을 읽는 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
