package com.project.common.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 미션/보상 도메인 전용 에러코드다. */
@Getter
@RequiredArgsConstructor
public enum MissionErrorCode implements BaseErrorCode {
    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "MISSION_001", "미션을 찾을 수 없습니다."),
    MISSION_NOT_ASSIGNED(HttpStatus.FORBIDDEN, "MISSION_002", "해당 미션은 본인에게 할당되지 않았습니다."),
    MISSION_OWNER_ONLY(HttpStatus.FORBIDDEN, "MISSION_003", "OWNER만 요청을 처리할 수 있습니다."),
    MISSION_INVALID_STATUS_TRANSITION(
            HttpStatus.BAD_REQUEST, "MISSION_004", "미션 상태 전이가 유효하지 않습니다."),
    MISSION_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "MISSION_005", "미션 요청을 찾을 수 없습니다."),
    MISSION_REQUEST_DUPLICATED(HttpStatus.CONFLICT, "MISSION_006", "이미 처리 대기 중인 요청이 존재합니다."),
    MISSION_REQUEST_INVALID_STATUS(HttpStatus.BAD_REQUEST, "MISSION_007", "요청 상태 전이가 유효하지 않습니다."),
    MISSION_REWARD_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "MISSION_008", "보상 템플릿을 찾을 수 없습니다."),
    MISSION_TARGET_NOT_IN_FAMILY(HttpStatus.BAD_REQUEST, "MISSION_009", "대상자가 같은 가족에 속하지 않습니다."),
    MISSION_INVALID_CURSOR(HttpStatus.BAD_REQUEST, "MISSION_010", "커서 형식이 유효하지 않습니다."),
    MISSION_TARGET_INVALID(HttpStatus.BAD_REQUEST, "MISSION_011", "미션 대상자가 유효하지 않습니다."),
    MISSION_REJECT_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "MISSION_012", "거절 사유는 필수입니다."),
    MISSION_INVALID_REQUEST_STATUS(HttpStatus.BAD_REQUEST, "MISSION_013", "요청 status가 유효하지 않습니다."),
    MISSION_REWARD_CATEGORY_MISMATCH(
            HttpStatus.BAD_REQUEST, "MISSION_014", "보상 카테고리가 템플릿과 일치하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
