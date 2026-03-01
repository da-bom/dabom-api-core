package com.project.domain.family.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.domain.usagerecord.dto.response.FamilyCustomersUsageResponse;
import com.project.domain.usagerecord.dto.response.FamilyUsageResponse;
import com.project.domain.usagerecord.model.FamilyCustomersUsage;
import com.project.domain.usagerecord.model.FamilyUsage;
import com.project.domain.usagerecord.service.UsageRecordService;
import com.project.global.api.response.ApiResponse;
import com.project.global.auth.aop.CustomerId;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/families")
@RequiredArgsConstructor
@Tag(name = "Family", description = "Customer 전용 가족 API")
public class FamilyController {

    private final UsageRecordService usageRecordService;

    @GetMapping(value = "/usage/current")
    @Operation(
            summary = "가족 구성원 현재 총 데이터 조회",
            description = "홈화면 상단부분에 속하는 가족 구성원의 총 데이터 사용량/제한량을 조회합니다.")
    public ApiResponse<FamilyUsageResponse> getCurrentFamilyUsage(
            @Parameter(hidden = true) @CustomerId Long customerId) {
        FamilyUsage familyUsage = usageRecordService.getCurrentFamilyUsage(customerId);
        FamilyUsageResponse response = FamilyUsageResponse.from(familyUsage);
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
        FamilyCustomersUsage familyCustomersUsage =
                usageRecordService.getCustomersUsageReport(customerId, year, month);
        FamilyCustomersUsageResponse response =
                FamilyCustomersUsageResponse.from(familyCustomersUsage);
        return ApiResponse.success(response);
    }
}
