package com.project.domain.policy.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.project.domain.customer.enums.RoleType;
import com.project.domain.policy.entity.Policy;
import com.project.domain.policy.enums.PolicyType;

public class PolicyResponse {

    public record ListResult(
            List<Detail> policies, int page, int size, long totalElements, int totalPages) {
        public static ListResult of(
                List<Detail> policies, int page, int size, long totalElements, int totalPages) {
            return new ListResult(policies, page, size, totalElements, totalPages);
        }
    }

    public record Summary(Long id, String name, String policyType, LocalDateTime createdAt) {
        public static Summary from(Policy policy) {
            return new Summary(
                    policy.getId(),
                    policy.getName(),
                    policy.getPolicyType().name(),
                    policy.getCreatedAt());
        }
    }

    public record Detail(
            Long id,
            String name,
            String description,
            PolicyType policyType,
            Map<String, Object> defaultRules,
            RoleType requiredRole,
            boolean isSystem,
            boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        public static Detail from(Policy policy) {
            return new Detail(
                    policy.getId(),
                    policy.getName(),
                    policy.getDescription(),
                    policy.getPolicyType(),
                    policy.getDefaultRules(),
                    policy.getRequiredRole(),
                    policy.isSystem(),
                    policy.isActive(),
                    policy.getCreatedAt(),
                    policy.getUpdatedAt());
        }
    }

    public record Create(
            Long id,
            String name,
            PolicyType policyType,
            boolean isSystem,
            LocalDateTime createdAt) {
        public static Create from(Policy policy) {
            return new Create(
                    policy.getId(),
                    policy.getName(),
                    policy.getPolicyType(),
                    policy.isSystem(),
                    policy.getCreatedAt());
        }
    }

    public record Updated(Long id, LocalDateTime updatedAt) {
        public static Updated from(Policy policy) {
            return new Updated(policy.getId(), policy.getUpdatedAt());
        }
    }
}
