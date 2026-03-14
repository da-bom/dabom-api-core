package com.project.common.exception;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.project.common.api.response.ApiResponse;
import com.project.common.exception.code.BaseErrorCode;
import com.project.common.exception.code.GlobalErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class ExceptionAdvice extends ResponseEntityExceptionHandler {

    /** BaseException - 도메인 예외 (ex: ApplicationException) */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Object> handleBaseException(BaseException e, HttpServletRequest request) {
        BaseErrorCode code = e.getCode();
        log.error("[BaseException] {} - {}", code.name(), code.getMessage());

        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResponse.fail(code.getCustomCode(), code.getMessage(), null));
    }

    /** 그 외 모든 예외 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnhandledException(Exception e, WebRequest request) {
        log.error("[Exception] Unhandled: {}", e.getMessage(), e);

        GlobalErrorCode code = GlobalErrorCode.INTERNAL_SERVER_ERROR;

        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResponse.fail(code.getCustomCode(), code.getMessage(), null));
    }
}
