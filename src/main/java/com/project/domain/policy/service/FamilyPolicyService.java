package com.project.domain.policy.service;

import java.util.Map;

import com.project.domain.policy.dto.response.FamilyPolicyResponse;
import com.project.domain.policy.enums.PolicyType;

public interface FamilyPolicyService {
    FamilyPolicyResponse getFamilyPolicyResponse(Long customerId);

    void updateMemberPolicy(
            Long targetCustomerId,
            PolicyType type,
            Map<String, Object> rules,
            Boolean isActive,
            Long actorId);
}
