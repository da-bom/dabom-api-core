package com.project.domain.usagerecord.model;

import java.util.List;

// UsageRecordService.getCustomersUsageReport에서 계산되어 가족 사용량 상세 조회 응답으로 전달되는 내부 모델
public record FamilyCustomersUsage(
        Long familyId,
        String familyName,
        int year,
        int month,
        Long totalQuotaBytes,
        Long remainingBytes,
        double usedPercent,
        List<CustomerUsage> customers) {}
