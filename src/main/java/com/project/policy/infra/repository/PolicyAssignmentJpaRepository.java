package com.project.policy.infra.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.policy.core.PolicyType;
import com.project.policy.infra.entity.PolicyAssignmentJpaEntity;

public interface PolicyAssignmentJpaRepository
        extends JpaRepository<PolicyAssignmentJpaEntity, Long> {
    @Query(
            "SELECT p FROM PolicyAssignmentJpaEntity p WHERE p.familyId = :familyId AND p.deletedAt"
                    + " IS NULL")
    List<PolicyAssignmentJpaEntity> findAllByFamilyId(@Param("familyId") Long familyId);

    @Query(
            "SELECT pa FROM PolicyAssignmentJpaEntity pa "
                    + "JOIN PolicyJpaEntity p ON pa.policyId = p.id "
                    + "WHERE pa.familyId = :familyId "
                    + "AND pa.targetCustomerId = :targetCustomerId "
                    + "AND p.type = :type "
                    + "AND pa.deletedAt IS NULL")
    Optional<PolicyAssignmentJpaEntity> findByTargetAndType(
            @Param("familyId") Long familyId,
            @Param("targetCustomerId") Long targetCustomerId,
            @Param("type") PolicyType type);
}
