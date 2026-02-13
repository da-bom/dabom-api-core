package com.project.policy.application.repository;

import java.util.List;
import java.util.Optional;

import com.project.policy.application.dto.FamilyPolicyDto;
import com.project.policy.core.PolicyAssignment;
import com.project.policy.core.PolicyType;

public interface PolicyQueryRepository {
    List<FamilyPolicyDto> findAllFamilyPoliciesByCustomerId(Long customerId);

    Optional<PolicyAssignment> findByTargetAndType(
            Long familyId, Long targetCustomerId, PolicyType type);

    Optional<Long> findFamilyIdByTargetCustomerId(Long customerId);
}
