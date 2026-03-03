package com.project.domain.usagerecord.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.project.domain.family.entity.Family;
import com.project.domain.family.repository.projection.FamilyUsageCustomerRow;
import com.project.domain.family.service.FamilyService;
import com.project.domain.family.util.FamilyUsageCalculator;
import com.project.domain.usagerecord.model.CustomerUsage;
import com.project.domain.usagerecord.model.FamilyCustomersUsage;
import com.project.domain.usagerecord.model.FamilyCustomersUsageSummary;
import com.project.domain.usagerecord.model.FamilyUsage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageRecordService {

    private final FamilyService familyService;

    // 현재 가족 데이터 사용량/제한량 조회
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

    // 홈 화면 하단용 - 가족 별 데이터 사용량/제한량 조회
    public FamilyCustomersUsageSummary getCustomersUsageSummaryReport(
            Long customerId, int year, int month) {
        Long familyId = familyService.getFamilyIdByCustomerId(customerId);
        LocalDate targetMonth = LocalDate.of(year, month, 1);
        List<FamilyUsageCustomerRow> rows =
                familyService.getUsageReportCustomers(familyId, targetMonth);
        List<CustomerUsage> customers = getCustomersUsage(rows, customerId);
        return new FamilyCustomersUsageSummary(familyId, year, month, customers);
    }

    // 파이 차트용 - 가족 별 데이터 분포 조회
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

    // 해당 월의 고객 데이터 정보 조회
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
