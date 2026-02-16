package com.project.domain.family.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.project.domain.family.dto.request.FamilySearchRequest;
import com.project.domain.family.dto.response.FamilyDetailResponse;
import com.project.domain.family.dto.response.FamilySearchResponse;
import com.project.domain.family.service.FamilyService;
import com.project.domain.usagerecord.service.UsageRecordService;
import com.project.global.api.response.ApiResponse;
import com.project.global.auth.aop.AdminOnly;
import com.project.global.auth.aop.CustomerId;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/families")
@RequiredArgsConstructor
@Tag(name = "Family", description = "가족 조회 API")
public class FamilyController {

    private final FamilyService familyService;
    private final UsageRecordService usageRecordService;

    @PostMapping
    @AdminOnly
    @Operation(summary = "가족 목록 검색", description = "페이지, 필터, 검색 조건으로 가족 목록을 조회합니다.")
    public ApiResponse<Page<FamilySearchResponse>> searchFamilies(
            @RequestBody FamilySearchRequest familySearchRequest) {
        Page<FamilySearchResponse> result = familyService.searchFamilies(familySearchRequest);
        return ApiResponse.success(result);
    }

    @GetMapping("/{familyId}")
    @AdminOnly
    @Operation(summary = "가족 상세 조회", description = "가족 ID로 가족 상세 정보를 조회합니다.")
    @Parameter(name = "familyId", description = "가족 ID", required = true)
    public ApiResponse<FamilyDetailResponse> getFamilyDetail(@PathVariable Long familyId) {
        FamilyDetailResponse result = familyService.getFamilyDetail(familyId);
        return ApiResponse.success(result);
    }

    @GetMapping(value = "/usage/current", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getCurrentUsage(@CustomerId Long customerId) {
        return usageRecordService.subscribe(customerId);
    }
}
