package com.project.policy.infra.repository;

import org.springframework.stereotype.Repository;

import com.project.policy.application.repository.PolicyCommandRepository;
import com.project.policy.core.PolicyAssignment;
import com.project.policy.infra.entity.PolicyAssignmentJpaEntity;
import com.project.policy.infra.mapper.PolicyAssignmentEntityMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PolicyCommandRepositoryImpl implements PolicyCommandRepository {
    private final PolicyAssignmentJpaRepository jpaRepository;
    private final PolicyAssignmentEntityMapper mapper;

    // --- Command (쓰기) ---
    @Override
    public PolicyAssignment save(PolicyAssignment domain) {
        PolicyAssignmentJpaEntity entity = mapper.toEntity(domain);
        return mapper.toDomain(jpaRepository.save(entity));
    }
}
