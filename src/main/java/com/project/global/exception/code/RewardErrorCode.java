package com.project.global.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RewardErrorCode implements BaseErrorCode {
    REWARD_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "REWARD_001", "보상 템플릿을 찾을 수 없습니다."),
    REWARD_TEMPLATE_SYSTEM_DELETE(HttpStatus.BAD_REQUEST, "REWARD_002", "시스템 보상 템플릿은 삭제할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
