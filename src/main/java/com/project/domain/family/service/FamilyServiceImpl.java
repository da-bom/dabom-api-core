package com.project.domain.family.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.family.dto.request.FamilySearchRequest;
import com.project.domain.family.dto.response.FamilyDetailResponse;
import com.project.domain.family.dto.response.FamilyMemberDetailResponse;
import com.project.domain.family.dto.response.FamilySearchResponse;
import com.project.domain.family.entity.Family;
import com.project.domain.family.infra.cache.FamilyCacheRepository;
import com.project.domain.family.repository.FamilyQueryRepository;
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
    public void handleFamilyEvent(Long familyId, Long customerId) {
        log.info("Handling family event: familyId={}, customerId={}", familyId, customerId);
    }
}
