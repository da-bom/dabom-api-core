package com.project.global.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UploadErrorCode implements BaseErrorCode {
    FILE_REQUIRED(HttpStatus.BAD_REQUEST, "UPLOAD_001", "파일이 없습니다."),
    INVALID_MIME_TYPE(HttpStatus.BAD_REQUEST, "UPLOAD_002", "지원하지 않는 파일 타입입니다."),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "UPLOAD_003", "파일 크기가 5MB를 초과합니다."),
    UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "UPLOAD_004", "파일 업로드에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
