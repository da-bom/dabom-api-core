package com.project.domain.policy.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.policy.dto.request.PolicyRequest;
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
    public Policy getPolicyDetail(Long policyId) {
        return policyRepository
                .findById(policyId)
                .orElseThrow(() -> new ApplicationException(PolicyErrorCode.POLICY_NOT_FOUND));
    }

    @Override
    public List<Policy> getPolicyList() {
        return policyRepository.findAll();
    }

    @Override
    @Transactional
    public Policy updatePolicy(Long policyId, PolicyRequest.Update updatePolicyRequest) {

        Policy policy =
                policyRepository
                        .findById(policyId)
                        .orElseThrow(
                                () -> new ApplicationException(PolicyErrorCode.POLICY_NOT_FOUND));

        if (!policy.isModifiable()) {
            throw new ApplicationException(PolicyErrorCode.POLICY_NOT_MODIFIABLE);
        }

        policy.update(
                updatePolicyRequest.description(),
                updatePolicyRequest.requiredRole(),
                updatePolicyRequest.policyType(),
                updatePolicyRequest.defaultRules(),
                updatePolicyRequest.isActive());

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
}
