package com.project.domain.policy.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.project.domain.policy.dto.request.PolicyRequest;
import com.project.domain.policy.entity.Policy;

public interface PolicyService {

    // 정책 생성 (어드민)
    Policy createPolicy(PolicyRequest.Create createPolicyRequest);

    // 정책 수정 (어드민)
    Policy updatePolicy(Long policyId, PolicyRequest.Update updatePolicyRequest);

    // 정책 상세 조회 (어드민)
    Policy getPolicyDetail(Long policyId);

    // 정책 리스트 조회 (어드민)
    Page<Policy> getPolicyList(Pageable pageable);
}
