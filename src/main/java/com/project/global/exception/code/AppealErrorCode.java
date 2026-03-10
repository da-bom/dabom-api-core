package com.project.global.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AppealErrorCode implements BaseErrorCode {
    APPEAL_NOT_FOUND(HttpStatus.NOT_FOUND, "APPEAL_001", "이의제기를 찾을 수 없습니다."),
    APPEAL_ALREADY_RESOLVED(HttpStatus.CONFLICT, "APPEAL_002", "이미 처리된 이의제기입니다."),
    APPEAL_FORBIDDEN(HttpStatus.FORBIDDEN, "APPEAL_003", "이의제기에 접근할 권한이 없습니다."),
    APPEAL_INVALID_DESIRED_RULES(
            HttpStatus.BAD_REQUEST, "APPEAL_004", "desiredRules 형식이 정책 유형과 맞지 않습니다."),
    APPEAL_EMERGENCY_MONTHLY_LIMIT(
            HttpStatus.TOO_MANY_REQUESTS, "APPEAL_005", "이번 달 긴급 요청을 이미 사용했습니다."),
    APPEAL_EMERGENCY_INVALID_BYTES(HttpStatus.BAD_REQUEST, "APPEAL_006", "요청 바이트가 허용 범위를 벗어났습니다."),
    APPEAL_EMERGENCY_UNLIMITED(
            HttpStatus.BAD_REQUEST, "APPEAL_007", "무제한 쿼터 사용자는 긴급 요청을 할 수 없습니다."),
    APPEAL_CANCEL_FORBIDDEN(HttpStatus.FORBIDDEN, "APPEAL_008", "본인이 생성한 이의제기만 취소할 수 있습니다."),
    APPEAL_NOT_CANCELLABLE(HttpStatus.CONFLICT, "APPEAL_009", "취소할 수 없는 상태입니다."),
    APPEAL_EMERGENCY_CANCEL_NOT_ALLOWED(
            HttpStatus.BAD_REQUEST, "APPEAL_010", "EMERGENCY 타입은 취소할 수 없습니다."),
    APPEAL_ALREADY_PENDING(HttpStatus.CONFLICT, "APPEAL_011", "같은 정책에 대해 진행 중인 이의제기가 이미 존재합니다.");

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
