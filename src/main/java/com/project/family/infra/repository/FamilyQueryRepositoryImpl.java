package com.project.family.infra.repository;

import static com.project.customer.infra.entity.QCustomerJpaEntity.customerJpaEntity;
import static com.project.customer.infra.entity.QCustomerQuotaJpaEntity.customerQuotaJpaEntity;
import static com.project.family.infra.entity.QFamilyJpaEntity.familyJpaEntity;
import static com.project.family.infra.entity.QFamilyMemberJpaEntity.familyMemberJpaEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.project.family.application.repository.FamilyQueryRepository;
import com.project.family.infra.entity.FamilyJpaEntity;
import com.project.family.web.dto.request.FamilySearchRequest;
import com.project.family.web.dto.response.FamilyDetailResponse;
import com.project.family.web.dto.response.FamilyMemberDetailResponse;
import com.project.family.web.dto.response.FamilyMemberSimpleResponse;
import com.project.family.web.dto.response.FamilySearchResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FamilyQueryRepositoryImpl implements FamilyQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<FamilySearchResponse> search(FamilySearchRequest request) {
        PageRequest pageable = PageRequest.of(request.getPage(), request.getSize());
        FamilySearchRequest.Filters filters = request.filters();

        // 1. 가족 정보 조회 (구성원 이름, 전화번호, 사용률 필터 적용)
        List<FamilyJpaEntity> families =
                queryFactory
                        .selectFrom(familyJpaEntity)
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
                        .select(familyJpaEntity.count())
                        .from(familyJpaEntity)
                        .where(
                                createMemberNameFilter(filters != null ? filters.name() : null),
                                createMemberPhoneFilter(filters != null ? filters.phone() : null),
                                createUsageRateFilter(filters != null ? filters.usageRate() : null))
                        .fetchOne();

        total = (total == null) ? 0L : total;

        List<Long> familyIds = families.stream().map(FamilyJpaEntity::getId).toList();
        Map<Long, List<FamilyMemberSimpleResponse>> membersMap = fetchMembersMap(familyIds);

        List<FamilySearchResponse> content =
                families.stream()
                        .map(
                                family ->
                                        new FamilySearchResponse(
                                                family.getId(),
                                                family.getName(),
                                                membersMap.getOrDefault(
                                                        family.getId(), Collections.emptyList()),
                                                family.getCreatedAt()))
                        .toList();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Optional<FamilyDetailResponse> findDetailById(Long familyId) {
        FamilyJpaEntity family =
                queryFactory
                        .selectFrom(familyJpaEntity)
                        .where(familyJpaEntity.id.eq(familyId))
                        .fetchOne();

        if (family == null){ return Optional.empty();}

        List<Tuple> results =
                queryFactory
                        .select(
                                customerJpaEntity.id,
                                customerJpaEntity.name,
                                familyMemberJpaEntity.role,
                                customerQuotaJpaEntity.monthlyLimitBytes,
                                customerQuotaJpaEntity.monthlyUsedBytes)
                        .from(familyMemberJpaEntity)
                        .join(customerJpaEntity)
                        .on(familyMemberJpaEntity.customerId.eq(customerJpaEntity.id))
                        .leftJoin(customerQuotaJpaEntity)
                        .on(
                                customerQuotaJpaEntity
                                        .customerId
                                        .eq(customerJpaEntity.id)
                                        .and(customerQuotaJpaEntity.familyId.eq(familyId))
                                        .and(
                                                customerQuotaJpaEntity.currentMonth.eq(
                                                        family.getCurrentMonth())))
                        .where(familyMemberJpaEntity.familyId.eq(familyId))
                        .fetch();

        List<FamilyMemberDetailResponse> customers =
                results.stream()
                        .map(
                                t ->
                                        new FamilyMemberDetailResponse(
                                                t.get(customerJpaEntity.id),
                                                t.get(customerJpaEntity.name),
                                                t.get(familyMemberJpaEntity.role),
                                                t.get(customerQuotaJpaEntity.monthlyLimitBytes),
                                                t.get(customerQuotaJpaEntity.monthlyUsedBytes)
                                                                != null
                                                        ? t.get(
                                                                customerQuotaJpaEntity
                                                                        .monthlyUsedBytes)
                                                        : 0L))
                        .toList();

        double usedPercent =
                family.getTotalQuotaBytes() > 0
                        ? (double) family.getUsedBytes() / family.getTotalQuotaBytes() * 100.0
                        : 0.0;

        return Optional.of(
                new FamilyDetailResponse(
                        family.getId(),
                        family.getName(),
                        family.getCreatedById(),
                        customers,
                        family.getTotalQuotaBytes(),
                        family.getUsedBytes(),
                        usedPercent,
                        family.getCurrentMonth(),
                        family.getCreatedAt(),
                        family.getUpdatedAt()));
    }

    /** 구성원 이름 필터 (서브쿼리) */
    private BooleanExpression createMemberNameFilter(FamilySearchRequest.StringCondition cond) {
        if (cond == null || cond.value() == null || cond.operator() == null){ return null;}

        BooleanExpression customerMatch =
                switch (cond.operator()) {
                    case "contains" -> customerJpaEntity.name.contains(cond.value());
                    case "eq" -> customerJpaEntity.name.eq(cond.value());
                    default -> null;
                };

        if (customerMatch == null){ return null;}

        return familyJpaEntity.id.in(
                JPAExpressions.select(familyMemberJpaEntity.familyId)
                        .from(familyMemberJpaEntity)
                        .join(customerJpaEntity)
                        .on(familyMemberJpaEntity.customerId.eq(customerJpaEntity.id))
                        .where(customerMatch));
    }

    /** 구성원 전화번호 필터 (서브쿼리) */
    private BooleanExpression createMemberPhoneFilter(FamilySearchRequest.StringCondition cond) {
        if (cond == null || cond.value() == null || cond.operator() == null){ return null;}

        BooleanExpression customerMatch =
                switch (cond.operator()) {
                    case "contains" -> customerJpaEntity.phoneNumber.contains(cond.value());
                    case "eq" -> customerJpaEntity.phoneNumber.eq(cond.value());
                    default -> null;
                };

        if (customerMatch == null){ return null;}

        return familyJpaEntity.id.in(
                JPAExpressions.select(familyMemberJpaEntity.familyId)
                        .from(familyMemberJpaEntity)
                        .join(customerJpaEntity)
                        .on(familyMemberJpaEntity.customerId.eq(customerJpaEntity.id))
                        .where(customerMatch));
    }

    /** 사용률 범위 필터 */
    private BooleanExpression createUsageRateFilter(FamilySearchRequest.RangeCondition cond) {
        if (cond == null || cond.operator() == null){ return null;}

        NumberExpression<Double> usageRate =
                familyJpaEntity
                        .usedBytes
                        .doubleValue()
                        .divide(familyJpaEntity.totalQuotaBytes)
                        .multiply(100.0);

        if ("between".equals(cond.operator())) {
            if (cond.min() != null && cond.max() != null)
                return usageRate.between(cond.min(), cond.max());
            if (cond.min() != null) return usageRate.goe(cond.min());
            if (cond.max() != null) return usageRate.loe(cond.max());
        }
        return null;
    }

    private OrderSpecifier<?> getSortOrder(List<FamilySearchRequest.SortCondition> sorts) {
        if (sorts == null || sorts.isEmpty()) {
            return new OrderSpecifier<>(Order.DESC, familyJpaEntity.id);
        }

        FamilySearchRequest.SortCondition sort = sorts.getFirst();
        Order order = "DESC".equalsIgnoreCase(sort.direction()) ? Order.DESC : Order.ASC;

        return switch (sort.field()) {
            case "usageRate" -> {
                NumberExpression<Double> rate =
                        familyJpaEntity
                                .usedBytes
                                .doubleValue()
                                .divide(familyJpaEntity.totalQuotaBytes)
                                .multiply(100.0);
                yield new OrderSpecifier<>(order, rate);
            }
            case "createdAt" -> new OrderSpecifier<>(order, familyJpaEntity.createdAt);
            default -> new OrderSpecifier<>(Order.DESC, familyJpaEntity.id);
        };
    }

    private Map<Long, List<FamilyMemberSimpleResponse>> fetchMembersMap(List<Long> familyIds) {
        if (familyIds.isEmpty()){ return Collections.emptyMap();}

        List<Tuple> results =
                queryFactory
                        .select(
                                familyMemberJpaEntity.familyId,
                                customerJpaEntity.id,
                                customerJpaEntity.name)
                        .from(familyMemberJpaEntity)
                        .join(customerJpaEntity)
                        .on(familyMemberJpaEntity.customerId.eq(customerJpaEntity.id))
                        .where(familyMemberJpaEntity.familyId.in(familyIds))
                        .fetch();

        return results.stream()
                .collect(
                        Collectors.groupingBy(
                                t -> t.get(familyMemberJpaEntity.familyId),
                                Collectors.mapping(
                                        t ->
                                                new FamilyMemberSimpleResponse(
                                                        t.get(customerJpaEntity.id),
                                                        t.get(customerJpaEntity.name)),
                                        Collectors.toList())));
    }
}