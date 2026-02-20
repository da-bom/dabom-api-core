package com.project.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.project.domain.customer.repository.CustomerQuotaRepository;
import com.project.domain.customer.repository.CustomerRepository;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.policy.repository.PolicyAssignmentRepository;
import com.project.domain.policy.repository.PolicyRepository;

@Component
public class BuilderSupporter {

    @Autowired private CustomerRepository customerRepository;

    @Autowired private FamilyMemberRepository familyMemberRepository;

    @Autowired private CustomerQuotaRepository customerQuotaRepository;

    // 정책 (Policy)
    @Autowired private PolicyRepository policyRepository;
    @Autowired private PolicyAssignmentRepository policyAssignmentRepository;

    public CustomerRepository customerRepository() {
        return customerRepository;
    }

    public FamilyMemberRepository familyMemberRepository() {
        return familyMemberRepository;
    }

    public CustomerQuotaRepository customerQuotaRepository() {
        return customerQuotaRepository;
    }

    public PolicyRepository policyRepository() {
        return policyRepository;
    }

    public PolicyAssignmentRepository policyAssignmentRepository() {
        return policyAssignmentRepository;
    }
}
