package com.project.domain.appeal.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.project.domain.policy.enums.PolicyType;

public record AppealablePolicyListResult(List<AppealablePolicy> policies) {

    public record AppealablePolicy(
            Long policyAssignmentId,
            Long policyId,
            String policyName,
            PolicyType policyType,
            Map<String, Object> appliedRules,
            boolean active,
            LocalDateTime appliedAt) {}
}
