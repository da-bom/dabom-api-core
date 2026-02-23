package com.project.domain.usagerecord.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.project.domain.family.entity.Family;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.family.repository.FamilyQueryRepository;
import com.project.domain.family.repository.FamilyRepository;
import com.project.domain.usagerecord.dto.response.CustomerUsage;
import com.project.domain.usagerecord.dto.response.FamilyCustomersUsageResponse;
import com.project.domain.usagerecord.dto.response.FamilyUsageResponse;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.FamilyErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageRecordService {

    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyQueryRepository familyQueryRepository;
    private final FamilyRepository familyRepository;

    // 현재 가족 데이터 사용량/제한량 조회
    public FamilyUsageResponse getCurrentFamilyUsage(Long customerId) {
        Long familyId = resolveFamilyId(customerId);

        Family familyEntity =
                familyRepository
                        .findById(familyId)
                        .orElseThrow(
                                () -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND));

        long totalQuotaBytes =
                familyEntity.getTotalQuotaBytes() != null ? familyEntity.getTotalQuotaBytes() : 0L;
        long totalUsedBytes =
                familyEntity.getUsedBytes() != null ? familyEntity.getUsedBytes() : 0L;
        long remainingBytes = Math.max(totalQuotaBytes - totalUsedBytes, 0L);

        return new FamilyUsageResponse(
                familyEntity.getId(), familyEntity.getName(), totalQuotaBytes, remainingBytes);
    }

    // 해당 년,월에 해당하는 가족 별 데이터 사용량/제한량 조회
    public FamilyCustomersUsageResponse getCustomersUsageReport(
            Long customerId, int year, int month) {
        Long familyId = resolveFamilyId(customerId);
        LocalDate targetMonth = LocalDate.of(year, month, 1);
        List<CustomerUsage> customers =
                getCustomersUsageByFamilyId(familyId, customerId, targetMonth);
        return new FamilyCustomersUsageResponse(familyId, year, month, customers);
    }

    // 가족 ID를 통해 구성원 List 데이터 조회
    private List<CustomerUsage> getCustomersUsageByFamilyId(
            Long familyId, Long customerId, LocalDate targetMonth) {
        return familyQueryRepository.findUsageReportCustomers(familyId, targetMonth).stream()
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

    // 고객 Id로 가족 Id 조회
    private Long resolveFamilyId(Long customerId) {
        return familyMemberRepository
                .findFamilyIdByCustomerId(customerId)
                .orElseThrow(() -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND));
    }
}
