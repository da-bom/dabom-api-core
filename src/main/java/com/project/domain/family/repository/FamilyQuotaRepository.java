package com.project.domain.family.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.domain.family.entity.FamilyQuota;

public interface FamilyQuotaRepository extends JpaRepository<FamilyQuota, Long> {

    @Query(
            """
            select familyQuota
            from FamilyQuota familyQuota
            where familyQuota.familyId = :familyId
              and familyQuota.currentMonth = :currentMonth
              and familyQuota.deletedAt is null
            """)
    Optional<FamilyQuota> findActiveByFamilyIdAndCurrentMonth(
            @Param("familyId") Long familyId, @Param("currentMonth") LocalDate currentMonth);
}
