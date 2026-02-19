package com.project.common.fixtures;

import com.project.domain.policy.entity.PolicyAssignment;

public class PolicyAssignmentFixtures {

    public static PolicyAssignment assignment(
            Long policyId, Long familyId, Long targetCustomerId, String rules, boolean isActive) {
        return PolicyAssignment.builder()
                .policyId(policyId)
                .familyId(familyId)
                .targetCustomerId(targetCustomerId)
                .rules(rules)
                .isActive(isActive)
                .build();
    }
}