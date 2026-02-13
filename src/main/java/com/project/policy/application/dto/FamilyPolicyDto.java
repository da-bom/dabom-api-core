package com.project.policy.application.dto;

import com.project.policy.core.PolicyType;
import com.querydsl.core.annotations.QueryProjection;

public record FamilyPolicyDto(
        Long familyId,
        Long customerId,
        String customerName,
        String phoneNumber,
        String role,
        Long assignmentId,
        Long policyId,
        String policyName,
        PolicyType type,
        boolean isActive,
        String rules) {
    @QueryProjection
    public FamilyPolicyDto {}
}
