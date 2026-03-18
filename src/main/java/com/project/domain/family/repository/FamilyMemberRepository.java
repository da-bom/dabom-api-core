package com.project.domain.family.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.project.domain.family.entity.FamilyMember;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {
    List<FamilyMember> findAllByFamilyIdAndDeletedAtIsNull(Long familyId);

    List<FamilyMember> findAllByFamilyIdAndCustomerIdInAndDeletedAtIsNull(
            Long familyId, List<Long> customerIds);

    Optional<FamilyMember> findByCustomerIdAndDeletedAtIsNull(Long customerId);

    boolean existsByCustomerIdAndDeletedAtIsNull(Long customerId);

    @Query(
            """
            select owner.customerId
            from FamilyMember member
            join FamilyMember owner on owner.familyId = member.familyId
            where member.customerId = :customerId
              and member.deletedAt is null
              and owner.deletedAt is null
              and owner.role = com.project.common.auth.enums.RoleType.OWNER
            """)
    List<Long> findActiveOwnerCustomerIdsByCustomerId(Long customerId);

    Optional<FamilyMember> findByFamilyIdAndCustomerIdAndDeletedAtIsNull(
            Long familyId, Long customerId);

    interface FamilyMemberTargetProjection {
        Long getFamilyId();

        Long getCustomerId();
    }

    @Query(
            "select f.familyId as familyId, f.customerId as customerId "
                    + "from FamilyMember f where f.familyId = :familyId and f.deletedAt is null")
    List<FamilyMemberTargetProjection> findAllActiveTargetsByFamilyId(Long familyId);

    @Query(
            "select f.familyId as familyId, f.customerId as customerId "
                    + "from FamilyMember f where f.deletedAt is null")
    List<FamilyMemberTargetProjection> findAllActiveTargets();

    @Query(
            "SELECT COUNT(DISTINCT fm.familyId) FROM FamilyMember fm"
                    + " WHERE fm.deletedAt IS NULL")
    long countDistinctActiveFamilies();
}
