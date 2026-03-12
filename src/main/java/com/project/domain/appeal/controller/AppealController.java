package com.project.domain.appeal.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.domain.appeal.dto.request.AppealCommentRequest;
import com.project.domain.appeal.dto.request.AppealCreateRequest;
import com.project.domain.appeal.dto.request.AppealRespondRequest;
import com.project.domain.appeal.dto.request.EmergencyQuotaRequest;
import com.project.domain.appeal.dto.response.AppealCancelResponse;
import com.project.domain.appeal.dto.response.AppealCommentResponse;
import com.project.domain.appeal.dto.response.AppealCreateResponse;
import com.project.domain.appeal.dto.response.AppealDetailResponse;
import com.project.domain.appeal.dto.response.AppealListResponse;
import com.project.domain.appeal.dto.response.AppealRespondResponse;
import com.project.domain.appeal.dto.response.EmergencyQuotaResponse;
import com.project.domain.appeal.enums.AppealStatus;
import com.project.domain.appeal.model.AppealCancelResult;
import com.project.domain.appeal.model.AppealCommentResult;
import com.project.domain.appeal.model.AppealCreateResult;
import com.project.domain.appeal.model.AppealDetailResult;
import com.project.domain.appeal.model.AppealListResult;
import com.project.domain.appeal.model.AppealRespondResult;
import com.project.domain.appeal.model.EmergencyQuotaResult;
import com.project.domain.appeal.service.AppealService;
import com.project.global.api.response.ApiResponse;
import com.project.global.auth.aop.CustomerId;
import com.project.global.auth.aop.OwnerOnly;
import com.project.global.auth.model.AuthContext;
import com.project.global.auth.service.AuthContextService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

/** 이의제기 API 컨트롤러 */
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

    /** 이의제기 상세 조회 처리 */
    @GetMapping("/{appealId}")
    @Operation(summary = "이의제기 상세 조회")
    public ApiResponse<AppealDetailResponse> getAppealDetail(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @PathVariable Long appealId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        AuthContext auth = authContextService.resolve(customerId);
        AppealDetailResult result = appealService.getAppealDetail(auth, appealId, cursor, size);
        return ApiResponse.success(AppealDetailResponse.from(result));
    }

    /** 이의제기 생성 처리 */
    @PostMapping
    @Operation(summary = "이의제기 생성")
    public ApiResponse<AppealCreateResponse> createAppeal(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @RequestBody @Valid AppealCreateRequest request) {
        AuthContext auth = authContextService.resolve(customerId);
        AppealCreateResult result = appealService.createAppeal(auth, request);
        return ApiResponse.created(AppealCreateResponse.from(result));
    }

    /** 이의제기 승인/거절 처리 */
    @OwnerOnly
    @PatchMapping("/{appealId}/respond")
    @Operation(summary = "이의제기 승인/거절")
    public ApiResponse<AppealRespondResponse> respondAppeal(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @PathVariable Long appealId,
            @RequestBody @Valid AppealRespondRequest request) {
        AuthContext auth = authContextService.resolve(customerId);
        AppealRespondResult result = appealService.respondAppeal(auth, appealId, request);
        return ApiResponse.success(AppealRespondResponse.from(result));
    }

    /** 이의제기 댓글 작성 처리 */
    @PostMapping("/{appealId}/comments")
    @Operation(summary = "이의제기 댓글 작성")
    public ApiResponse<AppealCommentResponse> createComment(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @PathVariable Long appealId,
            @RequestBody @Valid AppealCommentRequest request) {
        AuthContext auth = authContextService.resolve(customerId);
        AppealCommentResult result = appealService.createComment(auth, appealId, request);
        return ApiResponse.created(AppealCommentResponse.from(result));
    }

    /** 이의제기 취소 처리 */
    @PatchMapping("/{appealId}/cancel")
    @Operation(summary = "이의제기 취소")
    public ApiResponse<AppealCancelResponse> cancelAppeal(
            @Parameter(hidden = true) @CustomerId Long customerId, @PathVariable Long appealId) {
        AuthContext auth = authContextService.resolve(customerId);
        AppealCancelResult result = appealService.cancelAppeal(auth, appealId);
        return ApiResponse.success(AppealCancelResponse.from(result));
    }

    /** 긴급 쿼터 요청 처리 */
    @PostMapping("/emergency")
    @Operation(summary = "긴급 쿼터 요청")
    public ApiResponse<EmergencyQuotaResponse> requestEmergencyQuota(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @RequestBody @Valid EmergencyQuotaRequest request) {
        AuthContext auth = authContextService.resolve(customerId);
        EmergencyQuotaResult result = appealService.requestEmergencyQuota(auth, request);
        return ApiResponse.created(EmergencyQuotaResponse.from(result));
    }
}
