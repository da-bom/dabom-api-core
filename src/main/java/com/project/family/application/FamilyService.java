package com.project.family.application;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.family.application.repository.FamilyQueryRepository;
import com.project.family.core.Family;
import com.project.family.core.FamilyMember;
import com.project.family.infra.cache.FamilyCacheRepository;
import com.project.family.web.dto.request.FamilySearchRequest;
import com.project.family.web.dto.response.FamilyDetailResponse;
import com.project.family.web.dto.response.FamilyMemberDetailResponse;
import com.project.family.web.dto.response.FamilySearchResponse;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.FamilyErrorCode;

import lombok.RequiredArgsConstructor;

/** 가족 그룹 서비스 - 비즈니스 로직 판단은 Core 계층(Family)에 위임 - 여기서는 트랜잭션 관리와 흐름 제어만 담당 */
@Service
@RequiredArgsConstructor
public class FamilyService {

    private final FamilyQueryRepository familyQueryRepository;
    private final FamilyCacheRepository familyCacheRepository;

    @Transactional(readOnly = true)
    public Page<FamilySearchResponse> searchFamilies(FamilySearchRequest familySearchRequest) {
        return familyQueryRepository.search(familySearchRequest);
    }

    @Transactional(readOnly = true)
    public FamilyDetailResponse getFamilyDetail(Long familyId) {
        Family cachedFamily = familyCacheRepository.findById(familyId).orElse(null);

        FamilyDetailResponse dbResponse =
                familyQueryRepository
                        .findDetailById(familyId)
                        .orElseThrow(
                                () -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND));

        if (cachedFamily == null) {
            familyCacheRepository.save(toFamily(dbResponse));
            cachedFamily = toFamily(dbResponse);
        }

        Long totalQuotaBytes = cachedFamily.getTotalQuotaBytes();
        Long usedBytes = cachedFamily.getUsedBytes();

        if (totalQuotaBytes != null && totalQuotaBytes >= 0) {
            usedBytes =
                    familyCacheRepository
                            .findFamilyRemainingBytes(familyId)
                            .map(remaining -> Math.max(0L, totalQuotaBytes - remaining))
                            .orElse(usedBytes);
        }

        List<FamilyMemberDetailResponse> customers =
                dbResponse.customers().stream()
                        .map(
                                customer ->
                                        familyCacheRepository
                                                .findUserMonthlyUsageBytes(
                                                        familyId, customer.customerId())
                                                .map(
                                                        realtimeUsage ->
                                                                new FamilyMemberDetailResponse(
                                                                        customer.customerId(),
                                                                        customer.name(),
                                                                        customer.role(),
                                                                        customer
                                                                                .monthlyLimitBytes(),
                                                                        realtimeUsage))
                                                .orElse(customer))
                        .toList();

        double usedPercent =
                (totalQuotaBytes != null && totalQuotaBytes > 0)
                        ? (double) usedBytes / totalQuotaBytes * 100.0
                        : 0.0;

        return new FamilyDetailResponse(
                dbResponse.familyId(),
                cachedFamily.getName(),
                cachedFamily.getCreatedById(),
                customers,
                totalQuotaBytes,
                usedBytes,
                usedPercent,
                cachedFamily.getCurrentMonth(),
                dbResponse.createdAt(),
                dbResponse.updatedAt());
    }

    private Family toFamily(FamilyDetailResponse response) {
        List<FamilyMember> members =
                response.customers().stream()
                        .map(
                                customer ->
                                        FamilyMember.builder()
                                                .familyId(response.familyId())
                                                .customerId(customer.customerId())
                                                .role(customer.role())
                                                .build())
                        .toList();

        return Family.builder()
                .id(response.familyId())
                .name(response.familyName())
                .createdById(response.createdById())
                .totalQuotaBytes(response.totalQuotaBytes())
                .usedBytes(response.usedBytes())
                .currentMonth(response.currentMonth())
                .members(members)
                .build();
    }
}
