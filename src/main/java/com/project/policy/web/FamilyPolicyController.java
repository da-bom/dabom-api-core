package com.project.policy.web;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.customer.web.aop.CustomerId;
import com.project.global.api.response.ApiResponse;
import com.project.policy.application.PolicyService;
import com.project.policy.web.dto.request.PolicyUpdateRequest;
import com.project.policy.web.dto.response.FamilyPolicyResponse;
import com.project.policy.web.dto.response.PolicyUpdateResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/families/policies")
@RequiredArgsConstructor
public class FamilyPolicyController {
    private final PolicyService policyService;
    private final ObjectMapper objectMapper;

    /** 가족 구성원에게 적용되고 있는 정책 목록 조회 */
    @GetMapping
    public ApiResponse<FamilyPolicyResponse> getFamilyPolicies(@CustomerId Long customerId) {
        FamilyPolicyResponse response = policyService.getFamilyPolicyResponse(customerId);
        return ApiResponse.success(response);
    }

    /** 가족 구성원의 정책 부분 수정 */
    @PatchMapping
    public ApiResponse<PolicyUpdateResponse> updatePolicy(
            @CustomerId Long actorId, @RequestBody @Valid PolicyUpdateRequest request)
            throws JsonProcessingException {

        var updateInfo = request.update();
        String rulesJson =
                (updateInfo.rules() != null)
                        ? objectMapper.writeValueAsString(updateInfo.rules())
                        : null;

        policyService.updateMemberPolicy(
                updateInfo.customerId(),
                updateInfo.type(),
                rulesJson,
                updateInfo.isActive(),
                actorId);

        return ApiResponse.success(
                PolicyUpdateResponse.success(updateInfo.customerId(), updateInfo.type()));
    }
}
