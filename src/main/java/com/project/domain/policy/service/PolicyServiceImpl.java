package com.project.domain.policy.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.policy.dto.request.PolicyRequest;
import com.project.domain.policy.dto.response.PolicyResponse;
import com.project.domain.policy.entity.Policy;
import com.project.domain.policy.repository.PolicyRepository;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.PolicyErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;

    @Override
    public PolicyResponse.Detail getPolicyDetail(Long policyId) {
        Policy policy =
                policyRepository
                        .findById(policyId)
                        .orElseThrow(
                                () -> new ApplicationException(PolicyErrorCode.POLICY_NOT_FOUND));
        return PolicyResponse.Detail.from(policy);
    }

    @Override
    public List<PolicyResponse.Detail> getPolicyList() {
        return policyRepository.findAll().stream().map(PolicyResponse.Detail::from).toList();
    }

    @Override
    @Transactional
    public PolicyResponse.Updated updatePolicy(
            Long policyId, PolicyRequest.Update updatePolicyRequest) {
        Policy policy =
                policyRepository
                        .findById(policyId)
                        .orElseThrow(
                                () -> new ApplicationException(PolicyErrorCode.POLICY_NOT_FOUND));

        policy.update(
                updatePolicyRequest.description(),
                updatePolicyRequest.requiredRole(),
                updatePolicyRequest.policyType(),
                updatePolicyRequest.defaultRules(),
                updatePolicyRequest.isActive(),
                updatePolicyRequest.overWrite());

        return PolicyResponse.Updated.from(policy);
    }

    @Override
    @Transactional
    public PolicyResponse.Create createPolicy(PolicyRequest.Create createPolicyRequest) {
        Policy policy =
                Policy.builder()
                        .name(createPolicyRequest.name())
                        .policyType(createPolicyRequest.policyType())
                        .isSystem(false)
                        .isActive(true)
                        .overWrite(false)
                        .build();

        policyRepository.save(policy);

        return PolicyResponse.Create.from(policy);
    }
}
