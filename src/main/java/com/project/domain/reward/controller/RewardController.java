package com.project.domain.reward.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.domain.reward.dto.request.RespondRewardRequest;
import com.project.domain.reward.dto.response.ReceivedRewardListResponse;
import com.project.domain.reward.dto.response.RewardRespondResponse;
import com.project.domain.reward.model.ReceivedRewardListResult;
import com.project.domain.reward.model.RewardRespondResult;
import com.project.domain.reward.service.RewardService;
import com.project.global.api.response.ApiResponse;
import com.project.global.auth.aop.CustomerId;
import com.project.global.auth.aop.OwnerOnly;
import com.project.global.auth.model.AuthContext;
import com.project.global.auth.service.AuthContextService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

/** 미션 연계 보상 API 컨트롤러다. */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/rewards")
@Tag(name = "Reward", description = "미션 보상 API")
public class RewardController {

    private final RewardService rewardService;
    private final AuthContextService authContextService;

    /** OWNER가 보상 요청을 승인/거절한다. */
    @OwnerOnly
    @PutMapping("/requests/{requestId}/respond")
    @Operation(summary = "보상 요청 응답")
    public ApiResponse<RewardRespondResponse> respondRewardRequest(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @PathVariable Long requestId,
            @RequestBody @Valid RespondRewardRequest request) {
        AuthContext auth = authContextService.resolve(customerId);
        RewardRespondResult result = rewardService.respondRewardRequest(auth, requestId, request);
        return ApiResponse.success(RewardRespondResponse.from(result));
    }

    /** 본인이 승인받은 보상 수령 내역을 조회한다. */
    @GetMapping("/received")
    @Operation(summary = "보상 수령 내역 조회")
    public ApiResponse<ReceivedRewardListResponse> getReceivedRewards(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        AuthContext auth = authContextService.resolve(customerId);
        ReceivedRewardListResult result = rewardService.listReceivedRewards(auth, cursor, size);
        return ApiResponse.success(ReceivedRewardListResponse.from(result));
    }
}
