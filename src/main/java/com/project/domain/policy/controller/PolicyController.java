package com.project.domain.policy.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.domain.policy.dto.request.PolicyRequest;
import com.project.domain.policy.dto.response.PolicyResponse;
import com.project.domain.policy.entity.Policy;
import com.project.domain.policy.service.PolicyService;
import com.project.global.api.response.ApiResponse;
import com.project.global.auth.aop.AdminOnly;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping("/{policyId}")
    @AdminOnly
    @Parameters({@Parameter(name = "policyId", description = "Policy ID", required = true)})
    public ApiResponse<PolicyResponse.Detail> getPolicyDetail(@PathVariable Long policyId) {
        Policy policy = policyService.getPolicyDetail(policyId);
        return ApiResponse.success(PolicyResponse.Detail.from(policy));
    }

    @GetMapping
    @AdminOnly
    public ApiResponse<List<PolicyResponse.Detail>> getPolicyList() {
        List<PolicyResponse.Detail> response =
                policyService.getPolicyList().stream().map(PolicyResponse.Detail::from).toList();
        return ApiResponse.success(response);
    }

    @PostMapping
    @AdminOnly
    public ApiResponse<PolicyResponse.Create> createPolicy(
            @RequestBody PolicyRequest.Create policyRequest) {
        Policy policy = policyService.createPolicy(policyRequest);
        return ApiResponse.success(PolicyResponse.Create.from(policy));
    }

    @PutMapping("/{policyId}")
    @AdminOnly
    @Parameters({@Parameter(name = "policyId", description = "Policy ID", required = true)})
    public ApiResponse<PolicyResponse.Updated> updatePolicy(
            @PathVariable Long policyId, @RequestBody PolicyRequest.Update policyRequest) {
        Policy policy = policyService.updatePolicy(policyId, policyRequest);
        return ApiResponse.success(PolicyResponse.Updated.from(policy));
    }
}
