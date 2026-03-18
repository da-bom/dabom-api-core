package com.project.domain.family.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.project.common.auth.enums.RoleType;
import com.project.domain.family.entity.FamilyMember;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {
    List<FamilyMember> findAllByFamilyId(Long familyId);

    List<FamilyMember> findAllByFamilyIdAndDeletedAtIsNull(Long familyId);

    List<FamilyMember> findAllByFamilyIdAndCustomerIdInAndDeletedAtIsNull(
            Long familyId, List<Long> customerIds);

    Optional<FamilyMember> findByCustomerId(Long customerId);

    @Query("select f.role from FamilyMember f where f.customerId = :customerId")
    RoleType findRoleById(Long customerId);

    @Query("select fm.familyId from FamilyMember fm where fm.customerId = :customerId")
    Optional<Long> findFamilyIdByCustomerId(Long customerId);

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
