package com.project.domain.policy.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

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

        public static ListResult from(Page<Detail> page) {
            return new ListResult(
                    page.getContent(), // policies
                    page.getNumber(), // page ( 0-base page)
                    page.getSize(), // size
                    page.getTotalElements(), // totalElements
                    page.getTotalPages()); // totalPages
        }
    }

    public record Detail(
            Long policyId,
            String name,
            String description,
            PolicyType type,
            Map<String, Object> defaultRules,
            RoleType requireRole,
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
            Long policyId,
            String name,
            PolicyType type,
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

    public record Updated(Long policyId, LocalDateTime updatedAt) {
        public static Updated from(Policy policy) {
            return new Updated(policy.getId(), policy.getUpdatedAt());
        }
    }

    public record Deleted(Long policyId, LocalDateTime deletedAt) {
        public static Deleted from(Policy policy) {
            return new Deleted(policy.getId(), policy.getDeletedAt());
        }
    }
}
