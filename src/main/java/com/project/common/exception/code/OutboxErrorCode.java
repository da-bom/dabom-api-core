package com.project.common.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OutboxErrorCode implements BaseErrorCode {
    PAYLOAD_SERIALIZATION_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR, "OUTBOX_001", "정책 규칙 데이터가 손상되었습니다.");

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
