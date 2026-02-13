package com.project.policy.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.policy.application.dto.FamilyPolicyDto;
import com.project.policy.application.repository.PolicyCommandRepository;
import com.project.policy.application.repository.PolicyQueryRepository;
import com.project.policy.core.PolicyAssignment;
import com.project.policy.core.PolicyType;
import com.project.policy.web.dto.response.FamilyPolicyResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyCommandRepository policyCommandRepository;
    private final PolicyQueryRepository policyQueryRepository;
    private final ObjectMapper objectMapper;

    /** customerId가 속한 가족의 모든 정책 조회 */
    @Transactional(readOnly = true)
    public FamilyPolicyResponse getFamilyPolicyResponse(Long customerId) {

        // QueryDSL로 데이터 조회
        List<FamilyPolicyDto> flatList =
                policyQueryRepository.findAllFamilyPoliciesByCustomerId(customerId);
        // 응답 조립
        return FamilyPolicyResponse.from(flatList);
    }

    /** 특정 멤버의 특정 타입 정책 수정 */
    @Transactional
    public void updateMemberPolicy(
            Long targetCustomerId,
            PolicyType type,
            String newRules,
            Boolean isActive,
            Long actorId) {
        Long familyId =
                policyQueryRepository
                        .findFamilyIdByTargetCustomerId(actorId)
                        .orElseThrow(() -> new IllegalArgumentException("가족 정보를 찾을 수 없습니다."));
        PolicyAssignment assignment =
                policyQueryRepository
                        .findByTargetAndType(familyId, targetCustomerId, type)
                        .orElseThrow(() -> new IllegalArgumentException("해당 정책이 존재하지 않습니다."));
        if (newRules != null) {
            assignment.updateRules(newRules);
        }
        if (isActive != null) {
            assignment.toggleActive(isActive, actorId);
        }
        policyCommandRepository.save(assignment);
    }
}
