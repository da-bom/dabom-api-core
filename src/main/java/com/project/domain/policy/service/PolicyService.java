package com.project.domain.policy.service;

import java.util.List;

import com.project.domain.policy.dto.request.PolicyRequest;
import com.project.domain.policy.dto.response.PolicyResponse;

public interface PolicyService {

    PolicyResponse.Create createPolicy(PolicyRequest.Create createPolicyRequest);

    PolicyResponse.Updated updatePolicy(Long policyId, PolicyRequest.Update updatePolicyRequest);

    PolicyResponse.Detail getPolicyDetail(Long policyId);

    List<PolicyResponse.Detail> getPolicyList();
}
