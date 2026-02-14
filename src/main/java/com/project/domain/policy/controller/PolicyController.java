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
import com.project.domain.policy.service.PolicyService;
import com.project.global.api.response.ApiResponse;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping("/{policyId}")
    @Parameters({@Parameter(name = "policyId", description = "정책 ID", required = true)})
    public ApiResponse<PolicyResponse.Detail> getPolicyDetail(@PathVariable Long policyId) {
        return ApiResponse.success(policyService.getPolicyDetail(policyId));
    }

    @GetMapping
    public ApiResponse<List<PolicyResponse.Detail>> getPolicyList() {
        return ApiResponse.success(policyService.getPolicyList());
    }

    @PostMapping
    public ApiResponse<PolicyResponse.Create> createPolicy(
            @RequestBody PolicyRequest.Create policyRequest) {
        return ApiResponse.success(policyService.createPolicy(policyRequest));
    }

    @PutMapping("/{policyId}")
    @Parameters({@Parameter(name = "policyId", description = "정책 ID", required = true)})
    public ApiResponse<PolicyResponse.Updated> updatePolicy(
            @PathVariable Long policyId, @RequestBody PolicyRequest.Update policyRequest) {
        return ApiResponse.success(policyService.updatePolicy(policyId, policyRequest));
    }
}
