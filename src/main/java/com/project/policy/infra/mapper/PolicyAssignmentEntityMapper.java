package com.project.policy.infra.mapper;

import org.springframework.stereotype.Component;

import com.project.policy.core.PolicyAssignment;
import com.project.policy.infra.entity.PolicyAssignmentJpaEntity;

@Component
public class PolicyAssignmentEntityMapper {

    public PolicyAssignmentJpaEntity toEntity(PolicyAssignment domain) {
        return PolicyAssignmentJpaEntity.builder()
                .id(domain.getId())
                .policyId(domain.getPolicyId())
                .familyId(domain.getFamilyId())
                .targetCustomerId(domain.getTargetCustomerId())
                .rules(domain.getRules())
                .isActive(domain.isActive())
                .appliedAt(domain.getAppliedAt())
                .appliedById(domain.getAppliedById())
                .build();
    }

    public PolicyAssignment toDomain(PolicyAssignmentJpaEntity entity) {
        return PolicyAssignment.builder()
                .id(entity.getId())
                .policyId(entity.getPolicyId())
                .familyId(entity.getFamilyId())
                .targetCustomerId(entity.getTargetCustomerId())
                .rules(entity.getRules())
                .isActive(entity.isActive())
                .appliedAt(entity.getAppliedAt())
                .appliedById(entity.getAppliedById())
                .build();
    }
}
