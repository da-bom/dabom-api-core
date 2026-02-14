package com.project.domain.policy.service;

import com.project.domain.policy.dto.response.FamilyPolicyResponse;
import com.project.domain.policy.enums.PolicyType;

public interface FamilyPolicyService {
    FamilyPolicyResponse getFamilyPolicyResponse(Long customerId);

    void updateMemberPolicy(
            Long targetCustomerId,
            PolicyType type,
            String newRules,
            Boolean isActive,
            Long actorId);
}
