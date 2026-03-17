package com.project.domain.policy.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.domain.policy.entity.PolicyAssignment;
import com.project.domain.policy.enums.PolicyType;
import com.project.domain.policy.model.AppliedPolicyQueryResult;

public interface PolicyAssignmentRepository extends JpaRepository<PolicyAssignment, Long> {
    Optional<PolicyAssignment> findByIdAndDeletedAtIsNull(Long id);

    List<PolicyAssignment> findAllByIdInAndDeletedAtIsNull(Set<Long> ids);

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

    List<PolicyAssignment> findAllByTargetCustomerIdAndDeletedAtIsNull(Long customerId);

    @Query(
            "SELECT pa FROM PolicyAssignment pa "
                    + "JOIN Policy p ON pa.policyId = p.id "
                    + "WHERE pa.familyId = :familyId "
                    + "AND p.policyType = :type "
                    + "AND p.deletedAt IS NULL "
                    + "AND pa.deletedAt IS NULL")
    List<PolicyAssignment> findAllByFamilyIdAndType(
            @Param("familyId") Long familyId, @Param("type") PolicyType type);

    @Query(
            "SELECT pa FROM PolicyAssignment pa "
                    + "JOIN Policy p ON pa.policyId = p.id "
                    + "WHERE pa.familyId = :familyId "
                    + "AND pa.targetCustomerId IN :customerIds "
                    + "AND p.policyType = :type "
                    + "AND p.deletedAt IS NULL "
                    + "AND pa.deletedAt IS NULL")
    List<PolicyAssignment> findAllByFamilyIdAndCustomerIdsAndType(
            @Param("familyId") Long familyId,
            @Param("customerIds") List<Long> customerIds,
            @Param("type") PolicyType type);

    // 특정 고객에게 현재 적용 중인 정책 목록과 정책 메타 정보를 함께 조회
    @Query(
            """
            SELECT new com.project.domain.policy.model.AppliedPolicyQueryResult(
                pa.id,
                p.id,
                p.name,
                p.policyType,
                pa.rules,
                pa.isActive,
                pa.appliedAt
            )
            FROM PolicyAssignment pa
            JOIN Policy p ON p.id = pa.policyId
            WHERE pa.targetCustomerId = :customerId
              AND pa.deletedAt IS NULL
              AND p.deletedAt IS NULL
              AND pa.isActive = true
              AND p.isActive = true
            """)
    List<AppliedPolicyQueryResult> findAppealablePoliciesByCustomerId(
            @Param("customerId") Long customerId);
}
