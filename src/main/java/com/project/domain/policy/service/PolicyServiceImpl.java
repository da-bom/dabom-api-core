package com.project.domain.policy.service;

import java.util.Collections;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dabom.messaging.kafka.contract.KafkaEventTypes;
import com.dabom.messaging.kafka.contract.KafkaTopics;
import com.dabom.messaging.kafka.event.dto.policy.PolicyUpdatedPayload;
import com.dabom.messaging.kafka.event.publisher.KafkaEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.policy.dto.request.PolicyRequest;
import com.project.domain.policy.entity.Policy;
import com.project.domain.policy.repository.PolicyAssignmentRepository;
import com.project.domain.policy.repository.PolicyRepository;
import com.project.domain.policy.service.helper.RulesUtil;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.PolicyErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyAssignmentRepository policyAssignmentRepository;
    private final ObjectMapper objectMapper;

    private final KafkaEventPublisher kafkaEventPublisher;
    private final RulesUtil rulesUtil;

    // 정책 상세 정보 조회
    @Override
    public Policy getPolicyDetail(Long policyId) {
        return findPolicyOrThrow(policyId);
    }

    // 정책 리스트 조회
    @Override
    public Page<Policy> getPolicyList(Pageable pageable) {
        return policyRepository.findAllByDeletedAtIsNull(pageable);
    }

    @Override
    @Transactional
    public Policy updatePolicy(Long policyId, PolicyRequest.Update updatePolicyRequest) {
        Policy policy = findPolicyOrThrow(policyId);

        // 1) 정책 템플릿 정보 업데이트
        policy.update(
                updatePolicyRequest.description(),
                updatePolicyRequest.requireRole(),
                updatePolicyRequest.type(),
                updatePolicyRequest.defaultRules(),
                updatePolicyRequest.isActive());

        // 2) 덮어쓰기가 True일 경우 가족 구성원에게 부여된 정책들 즉시 수정
        if (updatePolicyRequest.overWrite()) {
            String policyKey = rulesUtil.toPolicyKey(policy.getPolicyType());

            try {
                String rule =
                        (policy.getDefaultRules() != null)
                                ? objectMapper.writeValueAsString(policy.getDefaultRules())
                                : null;
                applyToExistingAssignments(policy);
                kafkaEventPublisher.publish(
                        KafkaTopics.POLICY_UPDATED,
                        KafkaEventTypes.POLICY_UPDATED,
                        new PolicyUpdatedPayload(null, null, policyKey, rule, policy.isActive()));
            } catch (JsonProcessingException e) {
                throw new ApplicationException(PolicyErrorCode.POLICY_RULES_SERIALIZATION_FAILED);
            }
        }

        return policy;
    }

    @Override
    @Transactional
    public Policy createPolicy(PolicyRequest.Create createPolicyRequest) {
        Policy policy =
                Policy.builder()
                        .name(createPolicyRequest.name())
                        .policyType(createPolicyRequest.type())
                        .isSystem(false)
                        .isActive(true)
                        .build();

        policyRepository.save(policy);

        return policy;
    }

    @Override
    @Transactional
    public Policy deletePolicy(Long policyId) {
        Policy policy = findPolicyOrThrow(policyId);
        validateDeletable(policy);
        policy.delete();
        return policy;
    }

    // 공통 로직 : 정책 ID에 해당하는 정책(삭제가 안된) 조회
    private Policy findPolicyOrThrow(Long policyId) {
        return policyRepository
                .findByIdAndDeletedAtIsNull(policyId)
                .orElseThrow(() -> new ApplicationException(PolicyErrorCode.POLICY_NOT_FOUND));
    }

    // 공통 로직 : 기본 시스템 여부에 따라 삭제 가능/불가능 검증
    private void validateDeletable(Policy policy) {
        if (policy.isSystem()) {
            throw new ApplicationException(PolicyErrorCode.POLICY_NOT_DELETABLE);
        }
    }

    // 덮어쓰기가 true일 경우, 가족구성원에 부여된 정책들을 전부 조회하고 즉시 수정
    private void applyToExistingAssignments(Policy policy) {
        String newRules = convertRulesToJson(policy.getDefaultRules());

        policyAssignmentRepository.bulkUpdateAssignments(
                policy.getId(), newRules, policy.isActive());
    }

    // 정책 안 세부 규칙을 JSON으로 변환하는 메소드
    private String convertRulesToJson(Map<String, Object> defaultRules) {
        Map<String, Object> safeRules =
                defaultRules == null ? Collections.emptyMap() : defaultRules;
        try {
            return objectMapper.writeValueAsString(safeRules);
        } catch (JsonProcessingException e) {
            throw new ApplicationException(PolicyErrorCode.POLICY_RULES_SERIALIZATION_FAILED);
        }
    }
}
