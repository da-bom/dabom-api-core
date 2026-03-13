package com.project.domain.appeal.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.project.domain.appeal.model.AppealablePolicyListResult;
import com.project.domain.policy.enums.PolicyType;

public record AppealablePolicyListResponse(List<AppealablePolicyResponse> policies) {

    public static AppealablePolicyListResponse from(AppealablePolicyListResult result) {
        return new AppealablePolicyListResponse(
                result.policies().stream().map(AppealablePolicyResponse::from).toList());
    }

    public record AppealablePolicyResponse(
            Long policyAssignmentId,
            Long policyId,
            String policyName,
            PolicyType policyType,
            Map<String, Object> appliedRules,
            boolean active,
            LocalDateTime appliedAt) {

        public static AppealablePolicyResponse from(
                AppealablePolicyListResult.AppealablePolicy result) {
            return new AppealablePolicyResponse(
                    result.policyAssignmentId(),
                    result.policyId(),
                    result.policyName(),
                    result.policyType(),
                    result.appliedRules(),
                    result.active(),
                    result.appliedAt());
        }
    }
}
