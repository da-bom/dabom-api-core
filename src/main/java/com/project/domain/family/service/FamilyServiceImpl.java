package com.project.domain.family.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.family.dto.request.FamilySearchRequest;
import com.project.domain.family.infra.cache.FamilyCacheRepository;
import com.project.domain.family.model.FamilyDetail;
import com.project.domain.family.model.FamilyMemberDetail;
import com.project.domain.family.model.FamilySearchResult;
import com.project.domain.family.repository.FamilyQueryRepository;
import com.project.domain.family.util.FamilyUsageCalculator;
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
    public Page<FamilySearchResult> searchFamilies(FamilySearchRequest familySearchRequest) {
        return familyQueryRepository.search(familySearchRequest);
    }

    @Override
    public FamilyDetail getFamilyDetail(Long familyId) {
        // 가족 상세 데이터는 DB에서 조회
        FamilyDetail familyDetail =
                familyQueryRepository
                        .findDetailById(familyId)
                        .orElseThrow(
                                () -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND));

        Long totalQuotaBytes = familyDetail.totalQuotaBytes();

        List<FamilyMemberDetail> customers =
                familyDetail.customers().stream()
                        .map(
                                c ->
                                        // 구성원별 실시간 사용량은 Redis 값이 있으면 덮어씀
                                        familyCacheRepository
                                                .findCustomerMonthlyUsageBytes(
                                                        familyId, c.customerId())
                                                .map(
                                                        realtimeUsage ->
                                                                new FamilyMemberDetail(
                                                                        c.customerId(),
                                                                        c.name(),
                                                                        c.role(),
                                                                        c.monthlyLimitBytes(),
                                                                        realtimeUsage))
                                                .orElse(
                                                        new FamilyMemberDetail(
                                                                c.customerId(),
                                                                c.name(),
                                                                c.role(),
                                                                c.monthlyLimitBytes(),
                                                                c.monthlyUsedBytes())))
                        .toList();

        // 응답 usedBytes/usedPercent는 보정된 구성원 사용량 합계를 기준으로 계산
        long finalUsedBytes =
                customers.stream()
                        .map(FamilyMemberDetail::monthlyUsedBytes)
                        .filter(
                                monthlyUsedBytes ->
                                        monthlyUsedBytes != null && monthlyUsedBytes > 0)
                        .mapToLong(Long::longValue)
                        .sum();

        double usedPercent =
                FamilyUsageCalculator.calculateUsedPercent(finalUsedBytes, totalQuotaBytes);

        // 가족 메타 정보(name, quota, currentMonth 등)는 DB 조회 결과를 그대로 사용
        return new FamilyDetail(
                familyDetail.familyId(),
                familyDetail.familyName(),
                familyDetail.createdById(),
                customers,
                totalQuotaBytes,
                finalUsedBytes,
                usedPercent,
                familyDetail.currentMonth(),
                familyDetail.createdAt(),
                familyDetail.updatedAt());
    }

    @Override
    public void handleFamilyEvent(Long familyId, Long customerId) {
        log.info("Handling family event: familyId={}, customerId={}", familyId, customerId);
    }
}
