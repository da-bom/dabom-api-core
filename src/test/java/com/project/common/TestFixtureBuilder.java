package com.project.common;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.project.domain.customer.entity.Customer;
import com.project.domain.customer.entity.CustomerQuota;
import com.project.domain.family.entity.FamilyMember;
import com.project.domain.policy.entity.Policy;
import com.project.domain.policy.entity.PolicyAssignment;

@Component
public class TestFixtureBuilder {

    @Autowired private BuilderSupporter bs;

    public Customer buildCustomer(Customer customer) {
        return bs.customerRepository().save(customer);
    }

    public List<Customer> buildCustomers(List<Customer> customers) {
        return bs.customerRepository().saveAll(customers);
    }

    public void deleteCustomer(Customer customer) {
        bs.customerRepository().delete(customer);
    }

    public FamilyMember buildFamilyMember(FamilyMember familyMember) {
        return bs.familyMemberRepository().save(familyMember);
    }

    public List<FamilyMember> buildFamilyMembers(List<FamilyMember> familyMembers) {
        return bs.familyMemberRepository().saveAll(familyMembers);
    }

    public CustomerQuota buildCustomerQuota(CustomerQuota customerQuota) {
        return bs.customerQuotaRepository().save(customerQuota);
    }

    public List<CustomerQuota> buildCustomerQuotas(List<CustomerQuota> customerQuotas) {
        return bs.customerQuotaRepository().saveAll(customerQuotas);
    }

    public void deleteCustomerQuota(CustomerQuota customerQuota) {
        bs.customerQuotaRepository().delete(customerQuota);
    }


    // 정책 (Policy)
    public Policy buildPolicy(Policy policy) {
        return bs.policyRepository().save(policy);
    }
    public PolicyAssignment buildPolicyAssignment(PolicyAssignment policyAssignment) {
        return bs.policyAssignmentRepository().save(policyAssignment);
    }
    public List<PolicyAssignment> buildPolicyAssignments(List<PolicyAssignment> policyAssignments) {
        return bs.policyAssignmentRepository().saveAll(policyAssignments);
    }
}
