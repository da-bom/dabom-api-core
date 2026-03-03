package com.project.domain.usagerecord.service;

import com.project.domain.usagerecord.model.FamilyCustomersUsage;
import com.project.domain.usagerecord.model.FamilyCustomersUsageSummary;
import com.project.domain.usagerecord.model.FamilyUsage;

public interface UsageRecordService {
    // 현재 가족의 총 할당량/사용량을 조회한다.
    FamilyUsage getCurrentFamilyUsage(Long customerId);

    // 월 단위 가족 구성원 사용량 요약 목록을 조회한다.
    FamilyCustomersUsageSummary getCustomersUsageSummaryReport(
            Long customerId, int year, int month);

    // 월 단위 대시보드용 가족 사용량 상세 정보를 조회한다.
    FamilyCustomersUsage getCustomersUsageReport(Long customerId, int year, int month);
}
