package com.project.domain.policy.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.policy.dto.request.PolicyUpdateRequest;
import com.project.domain.policy.dto.response.FamilyPolicyResponse;
import com.project.domain.policy.dto.response.PolicyUpdateResponse;
import com.project.domain.policy.service.FamilyPolicyService;
import com.project.global.api.response.ApiResponse;
import com.project.global.auth.aop.CustomerId;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/families/policies")
@RequiredArgsConstructor
public class FamilyPolicyController {
    private final FamilyPolicyService familyPolicyService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ApiResponse<FamilyPolicyResponse> getFamilyPolicies(@CustomerId Long customerId) {
        FamilyPolicyResponse response = familyPolicyService.getFamilyPolicyResponse(customerId);
        return ApiResponse.success(response);
    }

    @PatchMapping
    public ApiResponse<PolicyUpdateResponse> updatePolicy(
            @CustomerId Long actorId, @RequestBody @Valid PolicyUpdateRequest request)
            throws JsonProcessingException {

        var updateInfo = request.update();
        String rulesJson =
                (updateInfo.rules() != null)
                        ? objectMapper.writeValueAsString(updateInfo.rules())
                        : null;

        familyPolicyService.updateMemberPolicy(
                updateInfo.customerId(),
                updateInfo.type(),
                rulesJson,
                updateInfo.isActive(),
                actorId);

        return ApiResponse.success(
                PolicyUpdateResponse.success(updateInfo.customerId(), updateInfo.type()));
    }
}
