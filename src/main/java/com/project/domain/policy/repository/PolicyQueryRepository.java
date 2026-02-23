package com.project.domain.policy.repository;

import static com.project.domain.customer.entity.QCustomer.customer;
import static com.project.domain.customer.entity.QCustomerQuota.customerQuota;
import static com.project.domain.family.entity.QFamily.family;
import static com.project.domain.family.entity.QFamilyMember.familyMember;
import static com.project.domain.policy.entity.QPolicy.policy;
import static com.project.domain.policy.entity.QPolicyAssignment.policyAssignment;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.project.domain.family.entity.QFamilyMember;
import com.project.domain.policy.dto.response.FamilyPolicyResponse;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PolicyQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<FamilyPolicyResponse.FlatPolicyRow> findAllFamilyPoliciesByCustomerId(
            Long customerId) {
        QFamilyMember subMember = new QFamilyMember("subMember");

        List<Tuple> results =
                queryFactory
                        .select(
                                familyMember.familyId,
                                familyMember.customerId,
                                customer.name,
                                customer.phoneNumber,
                                familyMember.role.stringValue(),
                                customerQuota.monthlyUsedBytes,
                                policyAssignment.id,
                                policy.id,
                                policy.name,
                                policy.policyType,
                                policyAssignment.isActive,
                                policyAssignment.rules)
                        .from(familyMember)
                        .join(customer)
                        .on(familyMember.customerId.eq(customer.id))
                        .join(family)
                        .on(familyMember.familyId.eq(family.id).and(family.deletedAt.isNull()))
                        .leftJoin(customerQuota)
                        .on(
                                customerQuota
                                        .familyId
                                        .eq(familyMember.familyId)
                                        .and(customerQuota.customerId.eq(familyMember.customerId))
                                        .and(customerQuota.currentMonth.eq(family.currentMonth))
                                        .and(customerQuota.deletedAt.isNull()))
                        .leftJoin(policyAssignment)
                        .on(
                                policyAssignment
                                        .familyId
                                        .eq(familyMember.familyId)
                                        .and(policyAssignment.deletedAt.isNull())
                                        .and(
                                                policyAssignment
                                                        .targetCustomerId
                                                        .eq(familyMember.customerId)
                                                        .or(
                                                                policyAssignment.targetCustomerId
                                                                        .isNull())))
                        .leftJoin(policy)
                        .on(policyAssignment.policyId.eq(policy.id))
                        .where(
                                familyMember
                                        .familyId
                                        .eq(
                                                JPAExpressions.select(subMember.familyId)
                                                        .from(subMember)
                                                        .where(
                                                                subMember
                                                                        .customerId
                                                                        .eq(customerId)
                                                                        .and(
                                                                                subMember.deletedAt
                                                                                        .isNull())))
                                        .and(familyMember.deletedAt.isNull()))
                        .orderBy(familyMember.customerId.asc(), policyAssignment.id.asc())
                        .fetch();

        return results.stream()
                .map(
                        t ->
                                new FamilyPolicyResponse.FlatPolicyRow(
                                        t.get(familyMember.familyId),
                                        t.get(familyMember.customerId),
                                        t.get(customer.name),
                                        t.get(customer.phoneNumber),
                                        t.get(familyMember.role.stringValue()),
                                        t.get(customerQuota.monthlyUsedBytes),
                                        t.get(policyAssignment.id),
                                        t.get(policy.id),
                                        t.get(policy.name),
                                        t.get(policy.policyType),
                                        t.get(policyAssignment.isActive),
                                        t.get(policyAssignment.rules)))
                .toList();
    }

    public Optional<Long> findFamilyIdByTargetCustomerId(Long customerId) {
        Long familyId =
                queryFactory
                        .select(familyMember.familyId)
                        .from(familyMember)
                        .where(
                                familyMember
                                        .customerId
                                        .eq(customerId)
                                        .and(familyMember.deletedAt.isNull()))
                        .fetchOne();
        return Optional.ofNullable(familyId);
    }
}
