package com.project.domain.family.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.family.dto.request.FamilySearchRequest;
import com.project.domain.family.entity.Family;
import com.project.domain.family.infra.cache.FamilyCacheRepository;
import com.project.domain.family.model.FamilyDetail;
import com.project.domain.family.model.FamilyMemberDetail;
import com.project.domain.family.model.FamilySearchResult;
import com.project.domain.family.model.FamilyUsageReport;
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
                (totalQuotaBytes != null && totalQuotaBytes > 0)
                        ? (double) finalUsedBytes / totalQuotaBytes * 100.0
                        : 0.0;

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
    public FamilyUsageReport getFamilyUsageReport(Long customerId, int year, int month) {
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

        List<FamilyUsageReport.CustomerUsage> customers =
                familyQueryRepository.findUsageReportCustomers(familyId, targetMonth).stream()
                        .map(
                                row ->
                                        new FamilyUsageReport.CustomerUsage(
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
                        .map(FamilyUsageReport.CustomerUsage::monthlyUsedBytes)
                        .filter(monthlyUsedBytes -> monthlyUsedBytes > 0)
                        .mapToLong(Long::longValue)
                        .sum();

        long totalQuotaBytes =
                familyEntity.getTotalQuotaBytes() != null ? familyEntity.getTotalQuotaBytes() : 0L;
        long remainingBytes = Math.max(totalQuotaBytes - totalUsedBytes, 0L);

        return new FamilyUsageReport(
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
