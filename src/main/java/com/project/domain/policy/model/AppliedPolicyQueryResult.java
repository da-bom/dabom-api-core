package com.project.domain.policy.model;

import java.time.LocalDateTime;

import com.project.domain.policy.enums.PolicyType;

/** 고객에게 적용 중인 정책 조회 결과 */
public record AppliedPolicyQueryResult(
        Long policyAssignmentId,
        Long policyId,
        String policyName,
        PolicyType policyType,
        String appliedRules,
        boolean active,
        LocalDateTime appliedAt) {}
