package com.project.domain.family.dto.response;

import java.util.List;

public record FamilyUsageReportResponse(
        Long familyId,
        String familyName,
        int year,
        int month,
        Long totalQuotaBytes,
        Long remainingBytes,
        List<CustomerUsage> customers) {

    public record CustomerUsage(
            Long customerId,
            String name,
            Long monthlyUsedBytes,
            Long monthlyLimitBytes,
            boolean isBlocked,
            String blockReason,
            boolean isMe) {}
}
