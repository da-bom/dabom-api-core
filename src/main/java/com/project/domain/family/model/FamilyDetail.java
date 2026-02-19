package com.project.domain.family.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FamilyDetail(
        Long familyId,
        String familyName,
        Long createdById,
        List<FamilyMemberDetail> customers,
        Long totalQuotaBytes,
        Long usedBytes,
        double usedPercent,
        LocalDate currentMonth,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
