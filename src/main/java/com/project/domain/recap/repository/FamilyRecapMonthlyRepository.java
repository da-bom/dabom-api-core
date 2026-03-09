package com.project.domain.recap.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.domain.recap.entity.FamilyRecapMonthly;

public interface FamilyRecapMonthlyRepository extends JpaRepository<FamilyRecapMonthly, Long> {
    Optional<FamilyRecapMonthly> findByFamilyIdAndReportMonthAndDeletedAtIsNull(
            Long familyId, LocalDate reportMonth);
}
