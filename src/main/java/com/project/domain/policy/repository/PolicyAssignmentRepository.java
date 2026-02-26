package com.project.domain.policy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.domain.policy.entity.PolicyAssignment;
import com.project.domain.policy.enums.PolicyType;

public interface PolicyAssignmentRepository extends JpaRepository<PolicyAssignment, Long> {
    @Query(
            "SELECT p FROM PolicyAssignment p WHERE p.familyId = :familyId AND p.deletedAt"
                    + " IS NULL")
    List<PolicyAssignment> findAllByFamilyId(@Param("familyId") Long familyId);

    @Query(
            "SELECT pa FROM PolicyAssignment pa "
                    + "JOIN Policy p ON pa.policyId = p.id "
                    + "WHERE pa.familyId = :familyId "
                    + "AND pa.targetCustomerId = :targetCustomerId "
                    + "AND p.policyType = :type "
                    + "AND p.deletedAt IS NULL "
                    + "AND pa.deletedAt IS NULL")
    Optional<PolicyAssignment> findByTargetAndType(
            @Param("familyId") Long familyId,
            @Param("targetCustomerId") Long targetCustomerId,
            @Param("type") PolicyType type);

    // 정책 ID에 해당하는 가족구성원에게 할당된 정책 전체 조회
    @Query(
            "SELECT pa FROM PolicyAssignment pa WHERE pa.policyId = :policyId AND pa.deletedAt IS"
                    + " NULL")
    List<PolicyAssignment> findAllByPolicyId(@Param("policyId") Long policyId);

    // 가족 구성원에 할당된 정책 전체 수정(Bulk를 활용하여 N+1문제 해결)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
        UPDATE PolicyAssignment pa
        SET pa.rules = :rules,
            pa.isActive = :active
        WHERE pa.policyId = :policyId
          AND pa.deletedAt IS NULL
        """)
    int bulkUpdateAssignments(
            @Param("policyId") Long policyId,
            @Param("rules") String rules,
            @Param("active") boolean active);
}
