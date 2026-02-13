package com.project.policy.application.repository;

import com.project.policy.core.PolicyAssignment;

public interface PolicyCommandRepository {
    PolicyAssignment save(PolicyAssignment policyAssignment);
}
