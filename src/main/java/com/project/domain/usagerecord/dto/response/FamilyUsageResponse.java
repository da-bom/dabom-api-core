package com.project.domain.usagerecord.dto.response;

import com.project.domain.usagerecord.model.FamilyUsage;

// 상단 화면 dto
public record FamilyUsageResponse(
        Long familyId, String familyName, Long totalQuotaBytes, Long remainingBytes) {
    public static FamilyUsageResponse from(FamilyUsage familyUsage) {
        return new FamilyUsageResponse(
                familyUsage.familyId(),
                familyUsage.familyName(),
                familyUsage.totalQuotaBytes(),
                familyUsage.remainingBytes());
    }
}
