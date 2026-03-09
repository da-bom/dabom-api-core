package com.project.domain.appeal.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.domain.appeal.dto.response.AppealListResponse;
import com.project.domain.appeal.enums.AppealStatus;
import com.project.domain.appeal.model.AppealListResult;
import com.project.domain.appeal.service.AppealService;
import com.project.global.api.response.ApiResponse;
import com.project.global.auth.aop.CustomerId;
import com.project.global.auth.model.AuthContext;
import com.project.global.auth.service.AuthContextService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/appeals")
@Tag(name = "Appeal", description = "이의제기 API")
public class AppealController {

    private final AppealService appealService;
    private final AuthContextService authContextService;

    /** 이의제기 목록 조회 처리 */
    @GetMapping
    @Operation(summary = "이의제기 목록 조회")
    public ApiResponse<AppealListResponse> getAppeals(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @RequestParam(required = false) AppealStatus status,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        AuthContext auth = authContextService.resolve(customerId);
        AppealListResult result = appealService.getAppeals(auth, status, cursor, size);
        return ApiResponse.success(AppealListResponse.from(result));
    }
}
