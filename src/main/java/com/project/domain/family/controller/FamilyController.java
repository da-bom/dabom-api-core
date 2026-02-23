package com.project.domain.family.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.domain.family.dto.request.FamilySearchRequest;
import com.project.domain.family.dto.response.FamilyDetailResponse;
import com.project.domain.family.dto.response.FamilySearchResponse;
import com.project.domain.family.model.FamilyDetail;
import com.project.domain.family.model.FamilySearchResult;
import com.project.domain.family.service.FamilyService;
import com.project.domain.usagerecord.dto.response.FamilyCustomersUsageResponse;
import com.project.domain.usagerecord.dto.response.FamilyUsageResponse;
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
        Page<FamilySearchResult> searchResult = familyService.searchFamilies(familySearchRequest);
        Page<FamilySearchResponse> result = searchResult.map(FamilySearchResponse::from);
        return ApiResponse.success(result);
    }

    @GetMapping("/{familyId}")
    @AdminOnly
    @Operation(summary = "가족 상세 조회", description = "가족 ID로 가족 상세 정보를 조회합니다.")
    public ApiResponse<FamilyDetailResponse> getFamilyDetail(
            @Parameter(description = "가족 ID", required = true) @PathVariable Long familyId) {
        FamilyDetail familyDetail = familyService.getFamilyDetail(familyId);
        return ApiResponse.success(FamilyDetailResponse.from(familyDetail));
    }

    @GetMapping(value = "/usage/current")
    @Operation(
            summary = "가족 구성원 현재 총 데이터 조회",
            description = "홈화면 상단부분에 속하는 가족 구성원의 총 데이터 사용량/제한량을 조회합니다.")
    public ApiResponse<FamilyUsageResponse> getCurrentFamilyUsage(
            @Parameter(hidden = true) @CustomerId Long customerId) {
        FamilyUsageResponse response = usageRecordService.getCurrentFamilyUsage(customerId);
        return ApiResponse.success(response);
    }

    @Validated
    @GetMapping("/usage/customers")
    @Operation(
            summary = "가족 데이터 상세 조회",
            description = "홈화면 하단부분에 속하는 Year, Month에 맞는 가족별 데이터 사용량/제한량을 조회합니다.")
    public ApiResponse<FamilyCustomersUsageResponse> getCustomersUsage(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @Parameter(description = "Year (yyyy)", required = true) @RequestParam @Min(2000)
                    int year,
            @Parameter(description = "Month (1-12)", required = true) @RequestParam @Min(1) @Max(12)
                    int month) {
        FamilyCustomersUsageResponse response =
                usageRecordService.getCustomersUsageReport(customerId, year, month);
        return ApiResponse.success(response);
    }
}
