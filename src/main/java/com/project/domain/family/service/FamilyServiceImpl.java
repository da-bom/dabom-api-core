package com.project.domain.family.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.family.dto.request.FamilySearchRequest;
import com.project.domain.family.dto.response.FamilyDetailResponse;
import com.project.domain.family.dto.response.FamilyMemberDetailResponse;
import com.project.domain.family.dto.response.FamilySearchResponse;
import com.project.domain.family.dto.response.FamilyUsageReportResponse;
import com.project.domain.family.entity.Family;
import com.project.domain.family.infra.cache.FamilyCacheRepository;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.family.repository.FamilyQueryRepository;
import com.project.domain.family.repository.FamilyRepository;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.FamilyErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FamilyServiceImpl implements FamilyService {

    private final FamilyQueryRepository familyQueryRepository;
    private final FamilyCacheRepository familyCacheRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyRepository familyRepository;

    @Override
    public Page<FamilySearchResponse> searchFamilies(FamilySearchRequest familySearchRequest) {
        return familyQueryRepository.search(familySearchRequest);
    }

    @Override
    public FamilyDetailResponse getFamilyDetail(Long familyId) {
        Family cachedFamily = familyCacheRepository.findById(familyId).orElse(null);

        FamilyDetailResponse dbResponse =
                familyQueryRepository
                        .findDetailById(familyId)
                        .orElseThrow(
                                () -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND));

        if (cachedFamily == null) {
            familyCacheRepository.save(dbResponse);
            cachedFamily = familyCacheRepository.findById(familyId).orElse(null);
        }

        Long totalQuotaBytes =
                cachedFamily != null
                        ? cachedFamily.getTotalQuotaBytes()
                        : dbResponse.totalQuotaBytes();

        List<FamilyMemberDetailResponse> customers =
                dbResponse.customers().stream()
                        .map(
                                c ->
                                        familyCacheRepository
                                                .findCustomerMonthlyUsageBytes(
                                                        familyId, c.customerId())
                                                .map(
                                                        realtimeUsage ->
                                                                new FamilyMemberDetailResponse(
                                                                        c.customerId(),
                                                                        c.name(),
                                                                        c.role(),
                                                                        c.monthlyLimitBytes(),
                                                                        realtimeUsage))
                                                .orElse(c))
                        .toList();

        long finalUsedBytes =
                customers.stream()
                        .map(FamilyMemberDetailResponse::monthlyUsedBytes)
                        .filter(
                                monthlyUsedBytes ->
                                        monthlyUsedBytes != null && monthlyUsedBytes > 0)
                        .mapToLong(Long::longValue)
                        .sum();

        double usedPercent =
                (totalQuotaBytes != null && totalQuotaBytes > 0)
                        ? (double) finalUsedBytes / totalQuotaBytes * 100.0
                        : 0.0;

        String familyName = cachedFamily != null ? cachedFamily.getName() : dbResponse.familyName();
        Long createdById =
                cachedFamily != null ? cachedFamily.getCreatedById() : dbResponse.createdById();

        return new FamilyDetailResponse(
                dbResponse.familyId(),
                familyName,
                createdById,
                customers,
                totalQuotaBytes,
                finalUsedBytes,
                usedPercent,
                cachedFamily != null ? cachedFamily.getCurrentMonth() : dbResponse.currentMonth(),
                dbResponse.createdAt(),
                dbResponse.updatedAt());
    }

    @Override
    public FamilyUsageReportResponse getFamilyUsageReport(Long customerId, int year, int month) {
        LocalDate targetMonth = LocalDate.of(year, month, 1);

        Long familyId =
                familyMemberRepository
                        .findFamilyIdByCustomerId(customerId)
                        .orElseThrow(
                                () -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND));

        Family familyEntity =
                familyRepository
                        .findById(familyId)
                        .orElseThrow(
                                () -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND));

        List<FamilyUsageReportResponse.CustomerUsage> customers =
                familyQueryRepository.findUsageReportCustomers(familyId, targetMonth).stream()
                        .map(
                                row ->
                                        new FamilyUsageReportResponse.CustomerUsage(
                                                row.customerId(),
                                                row.name(),
                                                row.monthlyUsedBytes(),
                                                row.monthlyLimitBytes(),
                                                row.isBlocked(),
                                                row.blockReason(),
                                                row.customerId().equals(customerId)))
                        .toList();

        long totalUsedBytes =
                customers.stream()
                        .map(FamilyUsageReportResponse.CustomerUsage::monthlyUsedBytes)
                        .filter(
                                monthlyUsedBytes ->
                                        monthlyUsedBytes != null && monthlyUsedBytes > 0)
                        .mapToLong(Long::longValue)
                        .sum();

        long totalQuotaBytes =
                familyEntity.getTotalQuotaBytes() != null ? familyEntity.getTotalQuotaBytes() : 0L;
        long remainingBytes = Math.max(totalQuotaBytes - totalUsedBytes, 0L);

        return new FamilyUsageReportResponse(
                familyEntity.getId(),
                familyEntity.getName(),
                year,
                month,
                totalQuotaBytes,
                remainingBytes,
                customers);
    }

    @Override
    public void handleFamilyEvent(Long familyId, Long customerId) {
        log.info("Handling family event: familyId={}, customerId={}", familyId, customerId);
    }
}
