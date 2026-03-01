package com.project.domain.policy.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.domain.policy.dto.request.PolicyRequest;
import com.project.domain.policy.dto.response.PolicyResponse;
import com.project.domain.policy.entity.Policy;
import com.project.domain.policy.service.PolicyService;
import com.project.global.api.response.ApiResponse;
import com.project.global.auth.aop.AdminOnly;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping("/{policyId}")
    @AdminOnly
    @Operation(summary = "정책 상세 조회", description = "Policy Id에 맞는 정책 템플릿을 조회합니다.")
    @Parameter(name = "policyId", description = "Policy ID", required = true)
    public ApiResponse<PolicyResponse.Detail> getPolicyDetail(@PathVariable Long policyId) {
        Policy policy = policyService.getPolicyDetail(policyId);
        return ApiResponse.success(PolicyResponse.Detail.from(policy));
    }

    @GetMapping
    @AdminOnly
    @Operation(summary = "정책 리스트 조회", description = "전체 정책 템플릿을 조회합니다.")
    public ApiResponse<PolicyResponse.ListResult> getPolicyList(
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Page<PolicyResponse.Detail> page =
                policyService.getPolicyList(pageable).map(PolicyResponse.Detail::from);
        PolicyResponse.ListResult response = PolicyResponse.ListResult.from(page);
        return ApiResponse.success(response);
    }

    @PostMapping
    @AdminOnly
    @Operation(summary = "정책 생성", description = "새로운 정책 템플릿을 생성합니다.")
    public ApiResponse<PolicyResponse.Create> createPolicy(
            @Valid @RequestBody PolicyRequest.Create policyRequest) {
        Policy policy = policyService.createPolicy(policyRequest);
        return ApiResponse.success(PolicyResponse.Create.from(policy));
    }

    @PutMapping("/{policyId}")
    @AdminOnly
    @Operation(summary = "정책 수정", description = "정책 템플릿을 수정합니다. overWrite에 따라 적용시점을 관리합니다.")
    @Parameter(name = "policyId", description = "Policy ID", required = true)
    public ApiResponse<PolicyResponse.Updated> updatePolicy(
            @PathVariable Long policyId, @Valid @RequestBody PolicyRequest.Update policyRequest)
            throws JsonProcessingException {
        Policy policy = policyService.updatePolicy(policyId, policyRequest);
        return ApiResponse.success(PolicyResponse.Updated.from(policy));
    }

    @DeleteMapping("/{policyId}")
    @AdminOnly
    @Operation(
            summary = "정책 삭제",
            description = "PolicyId에 맞는 정책템플릿을 삭제합니다. isSystem=true인 경우에는 삭제가 불가능합니다.")
    @Parameter(name = "policyId", description = "Policy ID", required = true)
    public ApiResponse<PolicyResponse.Deleted> deletePolicy(@PathVariable Long policyId) {
        Policy policy = policyService.deletePolicy(policyId);
        return ApiResponse.success(PolicyResponse.Deleted.from(policy));
    }
}
