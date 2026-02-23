package com.project.domain.policy.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.customer.enums.RoleType;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.policy.dto.response.FamilyPolicyResponse;
import com.project.domain.policy.entity.PolicyAssignment;
import com.project.domain.policy.enums.PolicyType;
import com.project.domain.policy.repository.PolicyAssignmentRepository;
import com.project.domain.policy.repository.PolicyQueryRepository;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.PolicyErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FamilyPolicyServiceImpl implements FamilyPolicyService {

    private final PolicyAssignmentRepository policyAssignmentRepository;
    private final PolicyQueryRepository policyQueryRepository;
    private final FamilyMemberRepository familyMemberRepository;

    @Override
    public FamilyPolicyResponse getFamilyPolicyResponse(Long customerId) {
        List<FamilyPolicyResponse.FlatPolicyRow> flatList =
                policyQueryRepository.findAllFamilyPoliciesByCustomerId(customerId);
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
            throw new IllegalArgumentException("가족장만 접근 가능합니다.");
        }

        Long familyId =
                policyQueryRepository
                        .findFamilyIdByTargetCustomerId(actorId)
                        .orElseThrow(() -> new IllegalArgumentException("가족 정보를 찾을 수 없습니다."));

        PolicyAssignment assignment =
                policyAssignmentRepository
                        .findByTargetAndType(familyId, targetCustomerId, type)
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                PolicyErrorCode.POLICY_ASSIGNMENT_NOT_FOUND));

        assignment.update(newRules, isActive, actorId);
    }
}
