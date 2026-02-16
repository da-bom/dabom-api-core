package com.project.domain.family.repository;

import static com.project.domain.customer.entity.QCustomer.customer;
import static com.project.domain.customer.entity.QCustomerQuota.customerQuota;
import static com.project.domain.family.entity.QFamily.family;
import static com.project.domain.family.entity.QFamilyMember.familyMember;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.project.domain.family.dto.request.FamilySearchRequest;
import com.project.domain.family.dto.response.FamilyDetailResponse;
import com.project.domain.family.dto.response.FamilyMemberDetailResponse;
import com.project.domain.family.dto.response.FamilyMemberSimpleResponse;
import com.project.domain.family.dto.response.FamilySearchResponse;
import com.project.domain.family.entity.Family;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.FamilyErrorCode;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FamilyQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<FamilySearchResponse> search(FamilySearchRequest request) {
        PageRequest pageable = PageRequest.of(request.getPage(), request.getSize());
        FamilySearchRequest.Filters filters = request.filters();

        List<Family> families =
                queryFactory
                        .selectFrom(family)
                        .where(
                                createMemberNameFilter(filters != null ? filters.name() : null),
                                createMemberPhoneFilter(filters != null ? filters.phone() : null),
                                createUsageRateFilter(filters != null ? filters.usageRate() : null))
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .orderBy(getSortOrder(request.sort()))
                        .fetch();

        Long total =
                queryFactory
                        .select(family.count())
                        .from(family)
                        .where(
                                createMemberNameFilter(filters != null ? filters.name() : null),
                                createMemberPhoneFilter(filters != null ? filters.phone() : null),
                                createUsageRateFilter(filters != null ? filters.usageRate() : null))
                        .fetchOne();

        total = (total == null) ? 0L : total;

        List<Long> familyIds = families.stream().map(Family::getId).toList();
        Map<Long, List<FamilyMemberSimpleResponse>> membersMap = fetchMembersMap(familyIds);

        List<FamilySearchResponse> content =
                families.stream()
                        .map(
                                f ->
                                        new FamilySearchResponse(
                                                f.getId(),
                                                f.getName(),
                                                membersMap.getOrDefault(
                                                        f.getId(), Collections.emptyList()),
                                                f.getCreatedAt()))
                        .toList();

        return new PageImpl<>(content, pageable, total);
    }

    public Optional<FamilyDetailResponse> findDetailById(Long familyId) {
        Family familyEntity =
                queryFactory.selectFrom(family).where(family.id.eq(familyId)).fetchOne();

        if (familyEntity == null) {
            return Optional.empty();
        }

        List<Tuple> results =
                queryFactory
                        .select(
                                customer.id,
                                customer.name,
                                familyMember.role,
                                customerQuota.monthlyLimitBytes,
                                customerQuota.monthlyUsedBytes)
                        .from(familyMember)
                        .join(customer)
                        .on(familyMember.customerId.eq(customer.id))
                        .leftJoin(customerQuota)
                        .on(
                                customerQuota
                                        .customerId
                                        .eq(customer.id)
                                        .and(customerQuota.familyId.eq(familyId))
                                        .and(
                                                customerQuota.currentMonth.eq(
                                                        familyEntity.getCurrentMonth())))
                        .where(familyMember.familyId.eq(familyId))
                        .fetch();

        List<FamilyMemberDetailResponse> customers =
                results.stream()
                        .map(
                                t ->
                                        new FamilyMemberDetailResponse(
                                                t.get(customer.id),
                                                t.get(customer.name),
                                                t.get(familyMember.role),
                                                t.get(customerQuota.monthlyLimitBytes),
                                                t.get(customerQuota.monthlyUsedBytes) != null
                                                        ? t.get(customerQuota.monthlyUsedBytes)
                                                        : 0L))
                        .toList();

        double usedPercent =
                familyEntity.getTotalQuotaBytes() > 0
                        ? (double) familyEntity.getUsedBytes()
                                / familyEntity.getTotalQuotaBytes()
                                * 100.0
                        : 0.0;

        return Optional.of(
                new FamilyDetailResponse(
                        familyEntity.getId(),
                        familyEntity.getName(),
                        familyEntity.getCreatedById(),
                        customers,
                        familyEntity.getTotalQuotaBytes(),
                        familyEntity.getUsedBytes(),
                        usedPercent,
                        familyEntity.getCurrentMonth(),
                        familyEntity.getCreatedAt(),
                        familyEntity.getUpdatedAt()));
    }

    public List<FamilyUsageCustomerRow> findUsageReportCustomers(Long familyId, LocalDate targetMonth) {
        List<Tuple> results =
                queryFactory
                        .select(
                                customer.id,
                                customer.name,
                                customerQuota.monthlyUsedBytes,
                                customerQuota.monthlyLimitBytes,
                                customerQuota.isBlocked,
                                customerQuota.blockReason)
                        .from(familyMember)
                        .join(customer)
                        .on(familyMember.customerId.eq(customer.id))
                        .leftJoin(customerQuota)
                        .on(
                                customerQuota
                                        .customerId
                                        .eq(customer.id)
                                        .and(customerQuota.familyId.eq(familyId))
                                        .and(customerQuota.currentMonth.eq(targetMonth)))
                        .where(familyMember.familyId.eq(familyId))
                        .fetch();

        return results.stream()
                .map(
                        tuple ->
                                new FamilyUsageCustomerRow(
                                        tuple.get(customer.id),
                                        tuple.get(customer.name),
                                        tuple.get(customerQuota.monthlyUsedBytes) != null
                                                ? tuple.get(customerQuota.monthlyUsedBytes)
                                                : 0L,
                                        tuple.get(customerQuota.monthlyLimitBytes),
                                        Boolean.TRUE.equals(tuple.get(customerQuota.isBlocked)),
                                        tuple.get(customerQuota.blockReason)))
                .toList();
    }

    private BooleanExpression createMemberNameFilter(FamilySearchRequest.StringCondition cond) {
        if (cond == null || cond.value() == null || cond.operator() == null) {
            return null;
        }

        BooleanExpression customerMatch =
                switch (cond.operator()) {
                    case "contains" -> customer.name.contains(cond.value());
                    case "eq" -> customer.name.eq(cond.value());
                    default -> throw invalidInput();
                };

        return family.id.in(
                JPAExpressions.select(familyMember.familyId)
                        .from(familyMember)
                        .join(customer)
                        .on(familyMember.customerId.eq(customer.id))
                        .where(customerMatch));
    }

    private BooleanExpression createMemberPhoneFilter(FamilySearchRequest.StringCondition cond) {
        if (cond == null || cond.value() == null || cond.operator() == null) {
            return null;
        }

        BooleanExpression customerMatch =
                switch (cond.operator()) {
                    case "contains" -> customer.phoneNumber.contains(cond.value());
                    case "eq" -> customer.phoneNumber.eq(cond.value());
                    default -> throw invalidInput();
                };

        return family.id.in(
                JPAExpressions.select(familyMember.familyId)
                        .from(familyMember)
                        .join(customer)
                        .on(familyMember.customerId.eq(customer.id))
                        .where(customerMatch));
    }

    private BooleanExpression createUsageRateFilter(FamilySearchRequest.RangeCondition cond) {
        if (cond == null) {
            return null;
        }

        if (cond.operator() == null || !"between".equalsIgnoreCase(cond.operator())) {
            throw invalidInput();
        }
        if (cond.min() == null && cond.max() == null) {
            throw invalidInput();
        }

        NumberExpression<Double> usageRate = usageRateExpression();

        if (cond.min() != null && cond.max() != null) {
            return usageRate.between(cond.min(), cond.max());
        }
        if (cond.min() != null) {
            return usageRate.goe(cond.min());
        }
        return usageRate.loe(cond.max());
    }

    private OrderSpecifier<?> getSortOrder(List<FamilySearchRequest.SortCondition> sorts) {
        if (sorts == null || sorts.isEmpty()) {
            return new OrderSpecifier<>(Order.DESC, family.id);
        }

        FamilySearchRequest.SortCondition sort = sorts.getFirst();

        if (sort.field() == null || sort.direction() == null) {
            throw invalidInput();
        }

        Order order =
                switch (sort.direction().toUpperCase()) {
                    case "ASC" -> Order.ASC;
                    case "DESC" -> Order.DESC;
                    default -> throw invalidInput();
                };

        return switch (sort.field()) {
            case "usageRate" -> new OrderSpecifier<>(order, usageRateExpression());
            case "createdAt" -> new OrderSpecifier<>(order, family.createdAt);
            default -> throw invalidInput();
        };
    }

    private Map<Long, List<FamilyMemberSimpleResponse>> fetchMembersMap(List<Long> familyIds) {
        if (familyIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Tuple> results =
                queryFactory
                        .select(familyMember.familyId, customer.id, customer.name)
                        .from(familyMember)
                        .join(customer)
                        .on(familyMember.customerId.eq(customer.id))
                        .where(familyMember.familyId.in(familyIds))
                        .fetch();

        return results.stream()
                .collect(
                        Collectors.groupingBy(
                                t -> t.get(familyMember.familyId),
                                Collectors.mapping(
                                        t ->
                                                new FamilyMemberSimpleResponse(
                                                        t.get(customer.id), t.get(customer.name)),
                                        Collectors.toList())));
    }

    private NumberExpression<Double> usageRateExpression() {
        return Expressions.numberTemplate(
                Double.class,
                "case when {0} = 0 then 0.0 else ({1} * 100.0 / {0}) end",
                family.totalQuotaBytes,
                family.usedBytes);
    }

    private ApplicationException invalidInput() {
        return new ApplicationException(FamilyErrorCode.FAMILY_INVALID_SEARCH_CONDITION);
    }

    public record FamilyUsageCustomerRow(
            Long customerId,
            String name,
            Long monthlyUsedBytes,
            Long monthlyLimitBytes,
            boolean isBlocked,
            String blockReason) {}
}
