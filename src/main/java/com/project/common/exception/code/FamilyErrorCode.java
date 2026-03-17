package com.project.common.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FamilyErrorCode implements BaseErrorCode {
    FAMILY_NOT_FOUND(HttpStatus.NOT_FOUND, "FAMILY_001", "가족 정보를 찾을 수 없습니다."),
    FAMILY_INVALID_SEARCH_CONDITION(HttpStatus.BAD_REQUEST, "FAMILY_002", "가족 검색 조건이 올바르지 않습니다."),
    FAMILY_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "FAMILY_003", "가족 구성원을 찾을 수 없습니다."),
    FAMILY_MEMBER_QUOTA_NOT_FOUND(HttpStatus.NOT_FOUND, "FAMILY_004", "구성원 쿼터 정보를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
