package com.project.global.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PolicyErrorCode implements BaseErrorCode {
    POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "POLICY_001", "정책을 찾을 수 없습니다."),
    POLICY_ASSIGNMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "POLICY_002", "해당 정책 할당을 찾을 수 없습니다."),
    POLICY_NOT_MODIFIABLE(HttpStatus.BAD_REQUEST, "POLICY_003", "수정할 수 없는 정책입니다."),
    POLICY_NOT_DELETABLE(HttpStatus.BAD_REQUEST, "POLICY_004", "삭제할 수 없는 정책입니다."),
    POLICY_RULES_SERIALIZATION_FAILED(HttpStatus.BAD_REQUEST, "POLICY_005", "상세 규칙 직렬화 과정 실패"),
    UNSUPPORTED_POLICY_TYPE(HttpStatus.BAD_REQUEST, "POLICY_006", "지원하지 않는 정책 타입입니다."),
    POLICY_OWNER_ONLY(HttpStatus.FORBIDDEN, "POLICY_007", "가족장(OWNER)만 정책을 수정할 수 있습니다."),
    POLICY_RULES_CORRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "POLICY_008", "정책 규칙 데이터가 손상되었습니다.");

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
