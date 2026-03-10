package com.project.domain.recap.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.domain.recap.dto.response.MonthlyRecapResponse;
import com.project.domain.recap.model.MonthlyRecap;
import com.project.domain.recap.service.RecapService;
import com.project.global.api.response.ApiResponse;
import com.project.global.auth.aop.CustomerId;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/recaps")
@RequiredArgsConstructor
@Tag(name = "Recap", description = "가족 리캡 조회 API")
public class RecapController {

    private final RecapService recapService;

    @GetMapping("/monthly")
    @Operation(summary = "월간 가족 리캡 조회", description = "로그인한 고객이 속한 가족의 월간 리캡을 조회합니다.")
    public ApiResponse<MonthlyRecapResponse> getMonthlyRecap(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @Parameter(description = "Year (yyyy)", required = true) @RequestParam @Min(2000)
                    int year,
            @Parameter(description = "Month (1-12)", required = true) @RequestParam @Min(1) @Max(12)
                    int month) {
        MonthlyRecap monthlyRecap = recapService.getMonthlyRecap(customerId, year, month);
        return ApiResponse.success(MonthlyRecapResponse.from(monthlyRecap));
    }
}
