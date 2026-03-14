package com.project.domain.usagerecord.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.family.entity.Family;
import com.project.domain.family.repository.projection.FamilyUsageCustomerRow;
import com.project.domain.family.service.FamilyService;
import com.project.domain.family.util.FamilyUsageCalculator;
import com.project.domain.usagerecord.model.CustomerUsage;
import com.project.domain.usagerecord.model.FamilyCustomersUsage;
import com.project.domain.usagerecord.model.FamilyCustomersUsageSummary;
import com.project.domain.usagerecord.model.FamilyUsage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsageRecordServiceImpl implements UsageRecordService {

    private final FamilyService familyService;

    // 현재 가족의 총 할당량/사용량을 조회한다.
    @Override
    public FamilyUsage getCurrentFamilyUsage(Long customerId) {
        Long familyId = familyService.getFamilyIdByCustomerId(customerId);
        Family familyEntity = familyService.getFamilyById(familyId);

        long totalQuotaBytes =
                familyEntity.getTotalQuotaBytes() != null ? familyEntity.getTotalQuotaBytes() : 0L;
        long totalUsedBytes =
                familyEntity.getUsedBytes() != null ? familyEntity.getUsedBytes() : 0L;

        return new FamilyUsage(
                familyEntity.getId(), familyEntity.getName(), totalQuotaBytes, totalUsedBytes);
    }

    // 월 단위 가족 구성원 사용량 요약 목록을 조회한다.
    @Override
    public FamilyCustomersUsageSummary getCustomersUsageSummaryReport(
            Long customerId, int year, int month) {
        Long familyId = familyService.getFamilyIdByCustomerId(customerId);
        LocalDate targetMonth = LocalDate.of(year, month, 1);
        List<FamilyUsageCustomerRow> rows =
                familyService.getUsageReportCustomers(familyId, targetMonth);
        List<CustomerUsage> customers = getCustomersUsage(rows, customerId);
        return new FamilyCustomersUsageSummary(familyId, year, month, customers);
    }

    // 월 단위 대시보드용 가족 사용량 상세 정보를 조회한다.
    @Override
    public FamilyCustomersUsage getCustomersUsageReport(Long customerId, int year, int month) {
        Long familyId = familyService.getFamilyIdByCustomerId(customerId);
        Family familyEntity = familyService.getFamilyById(familyId);
        LocalDate targetMonth = LocalDate.of(year, month, 1);
        List<FamilyUsageCustomerRow> rows =
                familyService.getUsageReportCustomers(familyId, targetMonth);

        long totalUsedBytes =
                rows.stream()
                        .map(FamilyUsageCustomerRow::monthlyUsedBytes)
                        .filter(
                                monthlyUsedBytes ->
                                        monthlyUsedBytes != null && monthlyUsedBytes > 0)
                        .mapToLong(Long::longValue)
                        .sum();

        long totalQuotaBytes =
                familyEntity.getTotalQuotaBytes() != null ? familyEntity.getTotalQuotaBytes() : 0L;

        long remainingBytes = Math.max(totalQuotaBytes - totalUsedBytes, 0L);
        double usedPercent =
                FamilyUsageCalculator.calculateUsedPercent(totalUsedBytes, totalQuotaBytes);

        List<CustomerUsage> customers = getCustomersUsage(rows, customerId);

        return new FamilyCustomersUsage(
                familyId,
                familyEntity.getName(),
                year,
                month,
                totalQuotaBytes,
                remainingBytes,
                usedPercent,
                customers);
    }

    // 조회 결과를 API 응답 모델용 고객 사용량 목록으로 변환한다.
    private List<CustomerUsage> getCustomersUsage(
            List<FamilyUsageCustomerRow> rows, Long customerId) {
        return rows.stream()
                .map(
                        row ->
                                new CustomerUsage(
                                        row.customerId(),
                                        row.name(),
                                        row.monthlyUsedBytes(),
                                        row.monthlyLimitBytes(),
                                        row.isBlocked(),
                                        row.blockReason(),
                                        row.customerId().equals(customerId)))
                .toList();
    }
}
