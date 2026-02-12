package com.project.family.web.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FamilyDetailResponse(
        Long familyId,
        String familyName,
        Long createdById,
        List<FamilyMemberDetailResponse> customers,
        Long totalQuotaBytes,
        Long usedBytes,
        double usedPercent,
        LocalDate currentMonth,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
