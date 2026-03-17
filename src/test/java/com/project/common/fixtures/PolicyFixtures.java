package com.project.common.fixtures;

import java.util.Map;

import com.project.common.auth.enums.RoleType;
import com.project.domain.policy.entity.Policy;
import com.project.domain.policy.enums.PolicyType;

public class PolicyFixtures {

    // 상세 규칙, 활성화 여부으로만 정책 생성
    public static Policy monthlyLimitPolicy(Map<String, Object> defaultRules, boolean isActive) {
        return monthlyLimitPolicy("monthly-limit-policy", defaultRules, isActive);
    }

    // 이름, 상세 규칙, 활성화 여부로 정책 생성
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

    // 모든 필드로 정책 생성
    public static Policy makePolicy(
            String name,
            String description,
            Map<String, Object> defaultRules,
            RoleType requiredRole,
            PolicyType policyType,
            boolean isSystem,
            boolean isActive) {
        return Policy.builder()
                .name(name)
                .description(description)
                .requiredRole(requiredRole)
                .policyType(policyType)
                .defaultRules(defaultRules)
                .isSystem(isSystem)
                .isActive(isActive)
                .build();
    }
}
