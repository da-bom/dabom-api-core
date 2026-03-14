package com.project.domain.reward.controller;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.common.api.response.ApiResponse;
import com.project.common.auth.aop.AdminOnly;
import com.project.domain.reward.dto.response.RewardGrantListResponse;
import com.project.domain.reward.entity.RewardGrant;
import com.project.domain.reward.enums.RewardGrantSort;
import com.project.domain.reward.enums.RewardGrantStatus;
import com.project.domain.reward.service.AdminRewardGrantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/rewards/grants")
@RequiredArgsConstructor
@Tag(name = "Admin_Reward_Grant", description = "관리자 보상 지급 내역 API")
public class AdminRewardGrantController {

    private final AdminRewardGrantService adminRewardGrantService;

    @GetMapping
    @AdminOnly
    @Operation(summary = "보상 지급 내역 조회", description = "관리자가 보상 지급 내역을 조회합니다.")
    public ApiResponse<RewardGrantListResponse> getGrants(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "지급 상태 필터") @RequestParam(required = false)
                    RewardGrantStatus status,
            @Parameter(description = "정렬 기준 (LATEST, EXPIRING_SOON)")
                    @RequestParam(defaultValue = "LATEST")
                    RewardGrantSort sort,
            @Parameter(description = "미사용 건만 조회 여부") @RequestParam(required = false)
                    Boolean unusedOnly,
            @Parameter(description = "전화번호 검색 (부분 일치)") @RequestParam(required = false)
                    String phoneNumber) {
        Page<RewardGrant> grants =
                adminRewardGrantService.getGrants(
                        page, size, status, sort, unusedOnly, phoneNumber);
        return ApiResponse.success(RewardGrantListResponse.from(grants));
    }
}
