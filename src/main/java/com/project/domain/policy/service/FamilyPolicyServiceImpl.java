package com.project.domain.policy.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dabom.messaging.kafka.contract.KafkaEventTypes;
import com.dabom.messaging.kafka.contract.KafkaTopics;
import com.dabom.messaging.kafka.event.dto.policy.PolicyUpdatedPayload;
import com.dabom.messaging.kafka.event.publisher.KafkaEventPublisher;
import com.project.common.auth.enums.RoleType;
import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.FamilyErrorCode;
import com.project.common.exception.code.PolicyErrorCode;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.policy.dto.response.FamilyPolicyResponse;
import com.project.domain.policy.entity.PolicyAssignment;
import com.project.domain.policy.enums.PolicyType;
import com.project.domain.policy.repository.PolicyAssignmentRepository;
import com.project.domain.policy.repository.PolicyQueryRepository;
import com.project.domain.policy.service.helper.RulesUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FamilyPolicyServiceImpl implements FamilyPolicyService {

    private final PolicyAssignmentRepository policyAssignmentRepository;
    private final PolicyQueryRepository policyQueryRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final RulesUtil rulesUtil;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final Clock clock;

    @Override
    public FamilyPolicyResponse getFamilyPolicyResponse(Long customerId) {
        List<FamilyPolicyResponse.FlatPolicyRow> flatList =
                policyQueryRepository.findAllFamilyPoliciesByCustomerId(customerId, currentMonth());
        return FamilyPolicyResponse.from(flatList);
    }

    @Override
    @Transactional
    public void updateMemberPolicy(
            Long targetCustomerId,
            PolicyType type,
            String newRules,
            Boolean isActive,
            Long actorId) {

        RoleType actorRole = familyMemberRepository.findRoleById(actorId);
        if (actorRole != RoleType.OWNER) {
            throw new ApplicationException(PolicyErrorCode.POLICY_OWNER_ONLY);
        }

        Long familyId =
                policyQueryRepository
                        .findFamilyIdByTargetCustomerId(actorId)
                        .orElseThrow(
                                () -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND));

        PolicyAssignment assignment =
                policyAssignmentRepository
                        .findByTargetAndType(familyId, targetCustomerId, type)
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                PolicyErrorCode.POLICY_ASSIGNMENT_NOT_FOUND));

        assignment.update(newRules, isActive, actorId);

        String policyKey = rulesUtil.toPolicyKey(type);
        kafkaEventPublisher.publish(
                KafkaTopics.POLICY_UPDATED,
                KafkaEventTypes.POLICY_UPDATED,
                new PolicyUpdatedPayload(
                        familyId, targetCustomerId, policyKey, newRules, isActive));
    }

    private LocalDate currentMonth() {
        return LocalDate.now(clock).withDayOfMonth(1);
    }
}
