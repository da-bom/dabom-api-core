package com.project.domain.usagerecord.model;

import java.util.List;

// UsageRecordService.getCustomersUsageSummaryReport에서 생성되어 구성원 목록 조회 응답으로 전달되는 내부 모델
public record FamilyCustomersUsageSummary(
        Long familyId, int year, int month, List<CustomerUsage> customers) {}
