package com.project.domain.policy.service;

import java.util.List;

import com.project.domain.policy.dto.request.PolicyRequest;
import com.project.domain.policy.entity.Policy;

public interface PolicyService {

    Policy createPolicy(PolicyRequest.Create createPolicyRequest);

    Policy updatePolicy(Long policyId, PolicyRequest.Update updatePolicyRequest);

    Policy getPolicyDetail(Long policyId);

    List<Policy> getPolicyList();
}
