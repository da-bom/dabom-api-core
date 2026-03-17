package com.project.domain.policy.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.common.api.response.ApiResponse;
import com.project.common.auth.aop.CustomerId;
import com.project.domain.policy.dto.request.PolicyUpdateRequest;
import com.project.domain.policy.dto.response.FamilyPolicyResponse;
import com.project.domain.policy.dto.response.PolicyUpdateResponse;
import com.project.domain.policy.service.FamilyPolicyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/families/policies")
@RequiredArgsConstructor
@Tag(name = "Family Policy", description = "가족 구성원 정책 조회/수정 API")
public class FamilyPolicyController {
    private final FamilyPolicyService familyPolicyService;

    @GetMapping
    @Operation(summary = "가족 정책 조회", description = "로그인한 고객이 속한 가족의 구성원별 정책 목록을 조회합니다.")
    public ApiResponse<FamilyPolicyResponse> getFamilyPolicies(
            @Parameter(hidden = true) @CustomerId Long customerId) {
        FamilyPolicyResponse response = familyPolicyService.getFamilyPolicyResponse(customerId);
        return ApiResponse.success(response);
    }

    @PatchMapping
    @Operation(summary = "구성원 정책 수정", description = "특정 구성원의 정책을 수정합니다.")
    public ApiResponse<PolicyUpdateResponse> updatePolicy(
            @Parameter(hidden = true) @CustomerId Long actorId,
            @RequestBody @Valid PolicyUpdateRequest request) {

        var updateInfo = request.updateInfo();
        familyPolicyService.updateMemberPolicy(
                updateInfo.customerId(),
                updateInfo.type(),
                updateInfo.rules(),
                updateInfo.isActive(),
                actorId);

        return ApiResponse.success(
                PolicyUpdateResponse.success(updateInfo.customerId(), updateInfo.type()));
    }
}
