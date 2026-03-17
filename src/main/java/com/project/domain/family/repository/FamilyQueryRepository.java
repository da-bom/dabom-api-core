package com.project.domain.family.repository;

import static com.project.domain.customer.entity.QCustomer.customer;
import static com.project.domain.customer.entity.QCustomerQuota.customerQuota;
import static com.project.domain.family.entity.QFamily.family;
import static com.project.domain.family.entity.QFamilyMember.familyMember;
import static com.project.domain.family.entity.QFamilyQuota.familyQuota;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.project.common.auth.enums.RoleType;
import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.FamilyErrorCode;
import com.project.domain.family.dto.request.FamilySearchRequest;
import com.project.domain.family.entity.Family;
import com.project.domain.family.entity.FamilyQuota;
import com.project.domain.family.model.FamilyDetail;
import com.project.domain.family.model.FamilyMemberDetail;
import com.project.domain.family.model.FamilyMemberInfo;
import com.project.domain.family.model.FamilyMemberSummary;
import com.project.domain.family.model.FamilySearchResult;
import com.project.domain.family.repository.projection.FamilyUsageCustomerRow;
import com.project.domain.family.util.FamilyUsageCalculator;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
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

    public Page<FamilySearchResult> search(FamilySearchRequest request, LocalDate targetMonth) {
        PageRequest pageable = PageRequest.of(request.getPage(), request.getSize());
        FamilySearchRequest.Filters filters = request.filters();

        List<Family> families =
                queryFactory
                        .selectFrom(family)
                        .leftJoin(familyQuota)
                        .on(
                                familyQuota
                                        .familyId
                                        .eq(family.id)
                                        .and(familyQuota.currentMonth.eq(targetMonth))
                                        .and(familyQuota.deletedAt.isNull()))
                        .where(
                                createMemberNameFilter(filters != null ? filters.name() : null),
                                createMemberPhoneFilter(filters != null ? filters.phone() : null),
                                createUsageRateFilter(filters != null ? filters.usageRate() : null),
                                family.deletedAt.isNull())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .orderBy(getSortOrder(request.sort()))
                        .fetch();

        Long total =
                queryFactory
                        .select(family.count())
                        .from(family)
                        .leftJoin(familyQuota)
                        .on(
                                familyQuota
                                        .familyId
                                        .eq(family.id)
                                        .and(familyQuota.currentMonth.eq(targetMonth))
                                        .and(familyQuota.deletedAt.isNull()))
                        .where(
                                createMemberNameFilter(filters != null ? filters.name() : null),
                                createMemberPhoneFilter(filters != null ? filters.phone() : null),
                                createUsageRateFilter(filters != null ? filters.usageRate() : null),
                                family.deletedAt.isNull())
                        .fetchOne();

        total = (total == null) ? 0L : total;

        List<Long> familyIds = families.stream().map(Family::getId).toList();
        Map<Long, List<FamilyMemberSummary>> membersMap = fetchMembersMap(familyIds);

        List<FamilySearchResult> content =
                families.stream()
                        .map(
                                f ->
                                        new FamilySearchResult(
                                                f.getId(),
                                                f.getName(),
                                                membersMap.getOrDefault(
                                                        f.getId(), Collections.emptyList()),
                                                f.getCreatedAt()))
                        .toList();

        return new PageImpl<>(content, pageable, total);
    }

    public Optional<FamilyDetail> findDetailById(Long familyId, LocalDate targetMonth) {
        Family familyEntity =
                queryFactory
                        .selectFrom(family)
                        .where(family.id.eq(familyId), family.deletedAt.isNull())
                        .fetchOne();

        if (familyEntity == null) {
            return Optional.empty();
        }

        FamilyQuota familyQuotaEntity =
                queryFactory
                        .selectFrom(familyQuota)
                        .where(
                                familyQuota.familyId.eq(familyId),
                                familyQuota.currentMonth.eq(targetMonth),
                                familyQuota.deletedAt.isNull())
                        .fetchOne();

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
                        .on(customer.id.eq(familyMember.customerId))
                        .leftJoin(customerQuota)
                        .on(
                                customerQuota
                                        .customerId
                                        .eq(customer.id)
                                        .and(customerQuota.familyId.eq(familyId))
                                        .and(customerQuota.currentMonth.eq(targetMonth))
                                        .and(customerQuota.deletedAt.isNull()))
                        .where(familyMember.familyId.eq(familyId), familyMember.deletedAt.isNull())
                        .fetch();

        List<FamilyMemberDetail> customers =
                results.stream()
                        .map(
                                tuple ->
                                        new FamilyMemberDetail(
                                                tuple.get(customer.id),
                                                tuple.get(customer.name),
                                                tuple.get(familyMember.role),
                                                tuple.get(customerQuota.monthlyLimitBytes),
                                                tuple.get(customerQuota.monthlyUsedBytes) != null
                                                        ? tuple.get(customerQuota.monthlyUsedBytes)
                                                        : 0L))
                        .toList();

        long usedBytes =
                customers.stream()
                        .map(FamilyMemberDetail::monthlyUsedBytes)
                        .filter(
                                monthlyUsedBytes ->
                                        monthlyUsedBytes != null && monthlyUsedBytes > 0)
                        .mapToLong(Long::longValue)
                        .sum();
        long totalQuotaBytes =
                familyQuotaEntity != null ? familyQuotaEntity.getTotalQuotaBytes() : 0L;
        double usedPercent = FamilyUsageCalculator.calculateUsedPercent(usedBytes, totalQuotaBytes);

        return Optional.of(
                new FamilyDetail(
                        familyEntity.getId(),
                        familyEntity.getName(),
                        familyEntity.getCreatedById(),
                        customers,
                        totalQuotaBytes,
                        usedBytes,
                        usedPercent,
                        targetMonth,
                        familyEntity.getCreatedAt(),
                        familyEntity.getUpdatedAt()));
    }

    public List<FamilyMemberInfo> findMembersByFamilyId(Long familyId) {
        return queryFactory
                .select(
                        Projections.constructor(
                                FamilyMemberInfo.class,
                                customer.id,
                                customer.name,
                                familyMember.role))
                .from(familyMember)
                .join(customer)
                .on(familyMember.customerId.eq(customer.id))
                .where(
                        familyMember.familyId.eq(familyId),
                        familyMember.role.eq(RoleType.MEMBER),
                        familyMember.deletedAt.isNull())
                .fetch();
    }

    public List<FamilyUsageCustomerRow> findUsageReportCustomers(
            Long familyId, LocalDate targetMonth) {
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
                                        .and(customerQuota.currentMonth.eq(targetMonth))
                                        .and(customerQuota.deletedAt.isNull()))
                        .where(familyMember.familyId.eq(familyId), familyMember.deletedAt.isNull())
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
                        .where(customerMatch, familyMember.deletedAt.isNull()));
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
                        .where(customerMatch, familyMember.deletedAt.isNull()));
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

        FamilySortField sortField = FamilySortField.fromValue(sort.field());

        return switch (sortField) {
            case USAGE_RATE -> new OrderSpecifier<>(order, usageRateExpression());
            case CREATED_AT -> new OrderSpecifier<>(order, family.createdAt);
            case NAME -> new OrderSpecifier<>(order, family.name);
        };
    }

    private enum FamilySortField {
        USAGE_RATE("usageRate"),
        CREATED_AT("createdAt"),
        NAME("name");

        private final String value;

        FamilySortField(String value) {
            this.value = value;
        }

        // 잘못된 값이면 예외 발생
        static FamilySortField fromValue(String value) {
            return Arrays.stream(values())
                    .filter(sortField -> sortField.value.equals(value))
                    .findFirst()
                    .orElseThrow(
                            () ->
                                    new ApplicationException(
                                            FamilyErrorCode.FAMILY_INVALID_SEARCH_CONDITION));
        }
    }

    private Map<Long, List<FamilyMemberSummary>> fetchMembersMap(@NonNull List<Long> familyIds) {
        if (familyIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Tuple> results =
                queryFactory
                        .select(familyMember.familyId, customer.id, customer.name)
                        .from(familyMember)
                        .join(customer)
                        .on(familyMember.customerId.eq(customer.id))
                        .where(familyMember.familyId.in(familyIds), familyMember.deletedAt.isNull())
                        .fetch();

        return results.stream()
                .collect(
                        Collectors.groupingBy(
                                tuple ->
                                        Objects.requireNonNull(
                                                tuple.get(familyMember.familyId),
                                                "familyId must not be null"),
                                Collectors.mapping(
                                        tuple ->
                                                new FamilyMemberSummary(
                                                        tuple.get(customer.id),
                                                        tuple.get(customer.name)),
                                        Collectors.toList())));
    }

    private NumberExpression<Double> usageRateExpression() {
        return Expressions.numberTemplate(
                Double.class,
                "case when coalesce({0}, 0) = 0 then 0.0 else (coalesce({1}, 0) * 100.0 /"
                        + " coalesce({0}, 0)) end",
                familyQuota.totalQuotaBytes,
                familyQuota.usedBytes);
    }

    private ApplicationException invalidInput() {
        return new ApplicationException(FamilyErrorCode.FAMILY_INVALID_SEARCH_CONDITION);
    }
}
