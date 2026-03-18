package com.project.domain.family.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.common.auth.enums.RoleType;
import com.project.domain.family.entity.FamilyMember;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {
    @Query("select fm from FamilyMember fm where fm.familyId = :familyId and fm.deletedAt is null")
    List<FamilyMember> findAllByFamilyId(@Param("familyId") Long familyId);

    List<FamilyMember> findAllByFamilyIdAndDeletedAtIsNull(Long familyId);

    List<FamilyMember> findAllByFamilyIdAndCustomerIdInAndDeletedAtIsNull(
            Long familyId, List<Long> customerIds);

    @Query(
            "select fm from FamilyMember fm where fm.customerId = :customerId and fm.deletedAt is"
                    + " null")
    Optional<FamilyMember> findByCustomerId(@Param("customerId") Long customerId);

    @Query(
            "select f.role from FamilyMember f where f.customerId = :customerId and f.deletedAt is"
                    + " null")
    RoleType findRoleById(@Param("customerId") Long customerId);

    @Query(
            "select fm.familyId from FamilyMember fm"
                    + " where fm.customerId = :customerId and fm.deletedAt is null")
    Optional<Long> findFamilyIdByCustomerId(@Param("customerId") Long customerId);

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
    List<Long> findActiveOwnerCustomerIdsByCustomerId(@Param("customerId") Long customerId);

    Optional<FamilyMember> findByFamilyIdAndCustomerIdAndDeletedAtIsNull(
            Long familyId, Long customerId);

    boolean existsByCustomerId(Long customerId);

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
