package com.project.domain.policy.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.policy.dto.request.PolicyRequest;
import com.project.domain.policy.entity.Policy;
import com.project.domain.policy.entity.PolicyAssignment;
import com.project.domain.policy.repository.PolicyAssignmentRepository;
import com.project.domain.policy.repository.PolicyRepository;
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

    // 정책 상세 정보 조회
    @Override
    public Policy getPolicyDetail(Long policyId) {
        return findPolicyOrThrow(policyId);
    }

    // 정책 리스트 조회
    @Override
    public List<Policy> getPolicyList() {
        return policyRepository.findAll();
    }

    @Override
    @Transactional
    public Policy updatePolicy(Long policyId, PolicyRequest.Update updatePolicyRequest) {
        Policy policy = findPolicyOrThrow(policyId);
        validateModifiable(policy);

        // 1) 정책 템플릿 정보 업데이트
        policy.update(
                updatePolicyRequest.description(),
                updatePolicyRequest.requiredRole(),
                updatePolicyRequest.policyType(),
                updatePolicyRequest.defaultRules(),
                updatePolicyRequest.isActive());

        // 2) 덮어쓰기가 True일 경우 가족 구성원에게 부여된 정책들 즉시 수정
        if (updatePolicyRequest.overWrite()) {
            applyToExistingAssignments(policy);
        }

        return policy;
    }

    @Override
    @Transactional
    public Policy createPolicy(PolicyRequest.Create createPolicyRequest) {
        Policy policy =
                Policy.builder()
                        .name(createPolicyRequest.name())
                        .policyType(createPolicyRequest.policyType())
                        .isSystem(false)
                        .isActive(true)
                        .build();

        policyRepository.save(policy);

        return policy;
    }

    // 공통 로직 : 정책 ID에 해당하는 정책 조회
    private Policy findPolicyOrThrow(Long policyId) {
        return policyRepository
                .findById(policyId)
                .orElseThrow(() -> new ApplicationException(PolicyErrorCode.POLICY_NOT_FOUND));
    }

    // 공통 로직 : 기본 시스템인지 여부에 따라 수정 가능/불가능 검증
    private void validateModifiable(Policy policy) {
        if (!policy.isModifiable()) {
            throw new ApplicationException(PolicyErrorCode.POLICY_NOT_MODIFIABLE);
        }
    }

    // 덮어쓰기가 true일 경우, 가족구성원에 부여된 정책들을 전부 조회하고 즉시 수정
    private void applyToExistingAssignments(Policy policy) {
        String newRules = convertRulesToJson(policy.getDefaultRules());
        List<PolicyAssignment> assignments =
                policyAssignmentRepository.findAllByPolicyId(policy.getId());

        for (PolicyAssignment assignment : assignments) {
            assignment.update(newRules, policy.isActive(), null);
        }
    }

    // 정책 안 세부 규칙을 JSON으로 변환하는 메소드
    private String convertRulesToJson(Map<String, Object> defaultRules) {
        Map<String, Object> safeRules = defaultRules == null ? Collections.emptyMap() : defaultRules;
        try {
            return objectMapper.writeValueAsString(safeRules);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize policy default rules", e);
        }
    }
}
