package com.project.domain.family.model;

import java.util.List;

// 순수 도메인 모델 (조회용 포함)
public record FamilyUsageReport(
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
