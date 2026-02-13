package com.project.policy.infra.mapper;

import org.springframework.stereotype.Component;

import com.project.policy.core.Policy;
import com.project.policy.infra.entity.PolicyJpaEntity;

@Component
public class PolicyEntityMapper {

    public PolicyJpaEntity toEntity(Policy domain) {
        return PolicyJpaEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .requireRole(domain.getRequireRole())
                .type(domain.getType())
                .defaultRules(domain.getDefaultRules())
                .isSystem(domain.isSystem())
                .build();
    }

    public Policy toDomain(PolicyJpaEntity entity) {
        return Policy.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .requireRole(entity.getRequireRole())
                .type(entity.getType())
                .defaultRules(entity.getDefaultRules())
                .isSystem(entity.isSystem())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }
}
