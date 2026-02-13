package com.project.policy.infra.repository;

import static com.project.family.infra.entity.QFamilyMemberJpaEntity.familyMemberJpaEntity;
import static com.project.policy.infra.entity.QPolicyJpaEntity.policyJpaEntity;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.project.customer.infra.entity.QCustomerJpaEntity;
import com.project.family.infra.entity.QFamilyMemberJpaEntity;
import com.project.policy.application.dto.FamilyPolicyDto;
import com.project.policy.application.dto.QFamilyPolicyDto;
import com.project.policy.application.repository.PolicyQueryRepository;
import com.project.policy.core.PolicyAssignment;
import com.project.policy.core.PolicyType;
import com.project.policy.infra.entity.QPolicyAssignmentJpaEntity;
import com.project.policy.infra.entity.QPolicyJpaEntity;
import com.project.policy.infra.mapper.PolicyAssignmentEntityMapper;
import com.project.policy.infra.mapper.PolicyEntityMapper;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PolicyQueryRepositoryImpl implements PolicyQueryRepository {

    private final PolicyAssignmentJpaRepository assignmentJpaRepository;
    private final PolicyJpaRepository policyJpaRepository;

    private final PolicyAssignmentEntityMapper assignmentMapper;
    private final PolicyEntityMapper policyMapper;

    private final JPAQueryFactory queryFactory;

    // --- Query (읽기) ---

    @Override
    public Optional<PolicyAssignment> findByTargetAndType(
            Long familyId, Long targetCustomerId, PolicyType type) {
        return assignmentJpaRepository
                .findByTargetAndType(familyId, targetCustomerId, type)
                .map(assignmentMapper::toDomain);
    }

    @Override
    public List<FamilyPolicyDto> findAllFamilyPoliciesByCustomerId(Long customerId) {
        QFamilyMemberJpaEntity member = familyMemberJpaEntity;
        QCustomerJpaEntity customer = QCustomerJpaEntity.customerJpaEntity;
        QPolicyAssignmentJpaEntity assignment =
                QPolicyAssignmentJpaEntity.policyAssignmentJpaEntity;
        QPolicyJpaEntity policy = policyJpaEntity;

        // 서브쿼리용 별칭
        QFamilyMemberJpaEntity subMember = new QFamilyMemberJpaEntity("subMember");
        return queryFactory
                .select(
                        new QFamilyPolicyDto(
                                member.familyId,
                                member.customerId,
                                customer.name,
                                customer.phoneNumber,
                                member.role.stringValue(),
                                assignment.id,
                                policy.id,
                                policy.name,
                                policy.type,
                                assignment.isActive,
                                assignment.rules))
                .from(member)
                .join(customer)
                .on(member.customerId.eq(customer.id))
                .leftJoin(assignment)
                .on(
                        assignment
                                .familyId
                                .eq(member.familyId)
                                .and(assignment.deletedAt.isNull())
                                .and(
                                        assignment
                                                .targetCustomerId
                                                .eq(member.customerId)
                                                .or(assignment.targetCustomerId.isNull())))
                .leftJoin(policy)
                .on(assignment.policyId.eq(policy.id))

                // 내 가족 ID와 같은 멤버들 조회 (SubQuery)
                .where(
                        member.familyId
                                .eq(
                                        JPAExpressions.select(subMember.familyId)
                                                .from(subMember)
                                                .where(
                                                        subMember
                                                                .customerId
                                                                .eq(customerId)
                                                                .and(subMember.deletedAt.isNull())))
                                .and(member.deletedAt.isNull()))
                .orderBy(member.customerId.asc(), assignment.id.asc())
                .fetch();
    }

    @Override
    public Optional<Long> findFamilyIdByTargetCustomerId(Long customerId) {
        Long familyId =
                queryFactory
                        .select(familyMemberJpaEntity.familyId)
                        .from(familyMemberJpaEntity)
                        .where(
                                familyMemberJpaEntity
                                        .customerId
                                        .eq(customerId)
                                        .and(familyMemberJpaEntity.deletedAt.isNull()))
                        .fetchOne();
        return Optional.ofNullable(familyId);
    }
}
