package com.project.common.fixtures;

import java.time.LocalDate;

import com.project.domain.family.entity.FamilyQuota;

public class FamilyQuotaFixtures {

    public static final Long DEFAULT_TOTAL_QUOTA = 10_000L;
    public static final LocalDate CURRENT_MONTH = LocalDate.now().withDayOfMonth(1);

    public static FamilyQuota quota(Long familyId, long usedBytes) {
        return FamilyQuota.builder()
                .familyId(familyId)
                .currentMonth(CURRENT_MONTH)
                .totalQuotaBytes(DEFAULT_TOTAL_QUOTA)
                .usedBytes(usedBytes)
                .build();
    }

    public static FamilyQuota quota(Long familyId, long totalQuotaBytes, long usedBytes) {
        return FamilyQuota.builder()
                .familyId(familyId)
                .currentMonth(CURRENT_MONTH)
                .totalQuotaBytes(totalQuotaBytes)
                .usedBytes(usedBytes)
                .build();
    }
}
