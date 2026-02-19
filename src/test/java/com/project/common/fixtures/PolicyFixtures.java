package com.project.common.fixtures;

import java.util.Map;

import com.project.domain.customer.enums.RoleType;
import com.project.domain.policy.entity.Policy;
import com.project.domain.policy.enums.PolicyType;

public class PolicyFixtures {

    public static Policy monthlyLimitPolicy(Map<String, Object> defaultRules, boolean isActive) {
        return monthlyLimitPolicy("monthly-limit-policy", defaultRules, isActive);
    }

    public static Policy monthlyLimitPolicy(
            String name, Map<String, Object> defaultRules, boolean isActive) {
        return Policy.builder()
                .name(name)
                .description("monthly limit policy")
                .requiredRole(RoleType.OWNER)
                .policyType(PolicyType.MONTHLY_LIMIT)
                .defaultRules(defaultRules)
                .isSystem(false)
                .isActive(isActive)
                .build();
    }
}
