package com.project.domain.usagerecord.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.project.domain.family.entity.Family;
import com.project.domain.family.service.FamilyService;
import com.project.domain.usagerecord.model.CustomerUsage;
import com.project.domain.usagerecord.model.FamilyCustomersUsage;
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
        long remainingBytes = Math.max(totalQuotaBytes - totalUsedBytes, 0L);

        return new FamilyUsage(
                familyEntity.getId(), familyEntity.getName(), totalQuotaBytes, remainingBytes);
    }

    // 해당 년,월에 해당하는 가족 별 데이터 사용량/제한량 조회
    public FamilyCustomersUsage getCustomersUsageReport(Long customerId, int year, int month) {
        Long familyId = familyService.getFamilyIdByCustomerId(customerId);
        LocalDate targetMonth = LocalDate.of(year, month, 1);
        List<CustomerUsage> customers =
                getCustomersUsageByFamilyId(familyId, customerId, targetMonth);
        return new FamilyCustomersUsage(familyId, year, month, customers);
    }

    // 가족 ID를 통해 구성원 List 데이터 조회
    private List<CustomerUsage> getCustomersUsageByFamilyId(
            Long familyId, Long customerId, LocalDate targetMonth) {
        return familyService.getUsageReportCustomers(familyId, targetMonth).stream()
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
