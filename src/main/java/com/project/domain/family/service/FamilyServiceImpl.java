package com.project.domain.family.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.FamilyErrorCode;
import com.project.common.exception.code.PolicyErrorCode;
import com.project.domain.customer.entity.CustomerQuota;
import com.project.domain.customer.repository.CustomerQuotaRepository;
import com.project.domain.family.dto.request.AdminFamilyUpdateRequest;
import com.project.domain.family.dto.request.FamilySearchRequest;
import com.project.domain.family.entity.Family;
import com.project.domain.family.entity.FamilyMember;
import com.project.domain.family.model.FamilyDetail;
import com.project.domain.family.model.FamilyMemberDetail;
import com.project.domain.family.model.FamilyMemberInfo;
import com.project.domain.family.model.FamilySearchResult;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.family.repository.FamilyQueryRepository;
import com.project.domain.family.repository.FamilyRepository;
import com.project.domain.family.repository.projection.FamilyUsageCustomerRow;
import com.project.domain.family.util.FamilyUsageCalculator;
import com.project.domain.policy.entity.PolicyAssignment;
import com.project.domain.policy.enums.PolicyType;
import com.project.domain.policy.repository.PolicyAssignmentRepository;
import com.project.domain.policy.service.helper.PolicyConstraintValueNormalizer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FamilyServiceImpl implements FamilyService {

    private final FamilyQueryRepository familyQueryRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyRepository familyRepository;
    private final CustomerQuotaRepository customerQuotaRepository;
    private final PolicyAssignmentRepository policyAssignmentRepository;
    private final PolicyConstraintValueNormalizer policyConstraintValueNormalizer;
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
                .findByCustomerIdAndDeletedAtIsNull(customerId)
                .map(FamilyMember::getFamilyId)
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

    @Override
    @Transactional
    public int updateFamilyByAdmin(
            Long familyId, List<AdminFamilyUpdateRequest.MemberUpdate> members) {
        validateNoDuplicateCustomerIds(members);
        getFamilyById(familyId);

        LocalDate targetMonth = currentMonth();

        List<Long> customerIds =
                members.stream().map(AdminFamilyUpdateRequest.MemberUpdate::customerId).toList();

        Map<Long, FamilyMember> memberMap =
                toEntityMap(
                        familyMemberRepository.findAllByFamilyIdAndCustomerIdInAndDeletedAtIsNull(
                                familyId, customerIds),
                        FamilyMember::getCustomerId);

        Map<Long, CustomerQuota> quotaMap =
                toEntityMap(
                        customerQuotaRepository
                                .findAllByFamilyIdAndCustomerIdInAndCurrentMonthAndDeletedAtIsNull(
                                        familyId, customerIds, targetMonth),
                        CustomerQuota::getCustomerId);

        Map<Long, PolicyAssignment> assignmentMap =
                toEntityMap(
                        policyAssignmentRepository.findAllByFamilyIdAndCustomerIdsAndType(
                                familyId, customerIds, PolicyType.MONTHLY_LIMIT),
                        PolicyAssignment::getTargetCustomerId);

        for (AdminFamilyUpdateRequest.MemberUpdate update : members) {
            Long customerId = update.customerId();

            FamilyMember member =
                    Optional.ofNullable(memberMap.get(customerId))
                            .orElseThrow(
                                    () ->
                                            new ApplicationException(
                                                    FamilyErrorCode.FAMILY_MEMBER_NOT_FOUND));
            member.changeRole(update.role());

            Optional.ofNullable(quotaMap.get(customerId))
                    .orElseThrow(
                            () ->
                                    new ApplicationException(
                                            FamilyErrorCode.FAMILY_MEMBER_QUOTA_NOT_FOUND))
                    .changeMonthlyLimitBytes(update.monthlyLimitBytes());

            PolicyAssignment assignment =
                    Optional.ofNullable(assignmentMap.get(customerId))
                            .orElseThrow(
                                    () ->
                                            new ApplicationException(
                                                    PolicyErrorCode.POLICY_ASSIGNMENT_NOT_FOUND));
            String newRules =
                    policyConstraintValueNormalizer.serializeMonthlyLimitRule(
                            update.monthlyLimitBytes());
            assignment.update(newRules, null, null);
        }

        return members.size();
    }

    private void validateNoDuplicateCustomerIds(
            List<AdminFamilyUpdateRequest.MemberUpdate> members) {
        long uniqueCount =
                members.stream()
                        .map(AdminFamilyUpdateRequest.MemberUpdate::customerId)
                        .distinct()
                        .count();
        if (uniqueCount != members.size()) {
            throw new ApplicationException(FamilyErrorCode.FAMILY_DUPLICATE_CUSTOMER_ID);
        }
    }

    private <T> Map<Long, T> toEntityMap(List<T> entities, Function<T, Long> keyExtractor) {
        return entities.stream().collect(Collectors.toMap(keyExtractor, Function.identity()));
    }

    private LocalDate currentMonth() {
        return LocalDate.now(clock).withDayOfMonth(1);
    }
}
