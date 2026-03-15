package com.project.domain.family.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.FamilyErrorCode;
import com.project.domain.family.dto.request.FamilySearchRequest;
import com.project.domain.family.entity.Family;
import com.project.domain.family.model.FamilyDetail;
import com.project.domain.family.model.FamilyMemberDetail;
import com.project.domain.family.model.FamilyMemberInfo;
import com.project.domain.family.model.FamilySearchResult;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.family.repository.FamilyQueryRepository;
import com.project.domain.family.repository.FamilyRepository;
import com.project.domain.family.repository.projection.FamilyUsageCustomerRow;
import com.project.domain.family.util.FamilyUsageCalculator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FamilyServiceImpl implements FamilyService {

    private final FamilyQueryRepository familyQueryRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyRepository familyRepository;
    private final Clock clock;

    @Override
    public Page<FamilySearchResult> searchFamilies(FamilySearchRequest familySearchRequest) {
        return familyQueryRepository.search(familySearchRequest, currentMonth());
    }

    @Override
    public FamilyDetail getFamilyDetail(Long familyId) {
        LocalDate targetMonth = currentMonth();
        FamilyDetail familyDetail =
                familyQueryRepository
                        .findDetailById(familyId, targetMonth)
                        .orElseThrow(
                                () -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND));

        Long totalQuotaBytes = familyDetail.totalQuotaBytes();
        List<FamilyMemberDetail> customers = familyDetail.customers();

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

        return new FamilyDetail(
                familyDetail.familyId(),
                familyDetail.familyName(),
                familyDetail.createdById(),
                customers,
                totalQuotaBytes,
                finalUsedBytes,
                usedPercent,
                targetMonth,
                familyDetail.createdAt(),
                familyDetail.updatedAt());
    }

    @Override
    public Long getFamilyIdByCustomerId(Long customerId) {
        return familyMemberRepository
                .findFamilyIdByCustomerId(customerId)
                .orElseThrow(() -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND));
    }

    @Override
    public Family getFamilyById(Long familyId) {
        return familyRepository
                .findById(familyId)
                .orElseThrow(() -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND));
    }

    @Override
    public List<FamilyUsageCustomerRow> getUsageReportCustomers(
            Long familyId, LocalDate targetMonth) {
        return familyQueryRepository.findUsageReportCustomers(familyId, targetMonth);
    }

    @Override
    public List<FamilyMemberInfo> getFamilyMembers(Long customerId) {
        Long familyId = getFamilyIdByCustomerId(customerId);
        return familyQueryRepository.findMembersByFamilyId(familyId);
    }

    @Override
    @Transactional
    public Family updateFamilyName(Long customerId, String name) {
        Long familyId = getFamilyIdByCustomerId(customerId);
        Family family = getFamilyById(familyId);
        family.changeName(name);
        return family;
    }

    private LocalDate currentMonth() {
        return LocalDate.now(clock).withDayOfMonth(1);
    }
}
