package com.project.family.infra.repository;

import static com.project.customer.infra.entity.QCustomerJpaEntity.customerJpaEntity;
import static com.project.family.infra.entity.QFamilyJpaEntity.familyJpaEntity;
import static com.project.family.infra.entity.QFamilyMemberJpaEntity.familyMemberJpaEntity;
import static com.project.quota.infra.entity.QCustomerQuotaJpaEntity.customerQuotaJpaEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.project.family.infra.entity.FamilyJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.project.family.application.repository.FamilyQueryRepository;
import com.project.family.web.dto.response.FamilyDetailResponse;
import com.project.family.web.dto.response.FamilyMemberDetailResponse;
import com.project.family.web.dto.response.FamilyMemberSimpleResponse;
import com.project.family.web.dto.response.FamilySearchResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FamilyQueryRepositoryImpl implements FamilyQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<FamilySearchResponse> search(String keyword, Pageable pageable) {
        // BooleanExpression 리팩토링 적용
        List<FamilyJpaEntity> families =
                queryFactory
                        .selectFrom(familyJpaEntity)
                        .where(searchKeywordContains(keyword))
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .orderBy(familyJpaEntity.id.desc())
                        .fetch();

        Long total =
                queryFactory
                        .select(familyJpaEntity.count())
                        .from(familyJpaEntity)
                        .where(searchKeywordContains(keyword))
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
                                                membersMap
                                                        .getOrDefault(
                                                                family.getId(),
                                                                Collections.emptyList())
                                                        .size(),
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

        if (family == null) return Optional.empty();

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

    // 📌 Helper Methods for BooleanExpression
    private BooleanExpression searchKeywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return familyNameContains(keyword).or(memberInfoContains(keyword));
    }

    private BooleanExpression familyNameContains(String keyword) {
        return familyJpaEntity.name.contains(keyword);
    }

    private BooleanExpression memberInfoContains(String keyword) {
        return familyJpaEntity.id.in(
                JPAExpressions.select(familyMemberJpaEntity.familyId)
                        .from(familyMemberJpaEntity)
                        .join(customerJpaEntity)
                        .on(familyMemberJpaEntity.customerId.eq(customerJpaEntity.id))
                        .where(
                                customerJpaEntity
                                        .name
                                        .contains(keyword)
                                        .or(customerJpaEntity.phoneNumber.contains(keyword))));
    }

    private Map<Long, List<FamilyMemberSimpleResponse>> fetchMembersMap(List<Long> familyIds) {
        if (familyIds.isEmpty()) return Collections.emptyMap();

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
