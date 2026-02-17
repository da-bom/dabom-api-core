package com.project.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.project.domain.customer.repository.CustomerQuotaRepository;
import com.project.domain.customer.repository.CustomerRepository;
import com.project.domain.family.repository.FamilyMemberRepository;

@Component
public class BuilderSupporter {

    @Autowired private CustomerRepository customerRepository;

    @Autowired private FamilyMemberRepository familyMemberRepository;

    @Autowired private CustomerQuotaRepository customerQuotaRepository;

    public CustomerRepository customerRepository() {
        return customerRepository;
    }

    public FamilyMemberRepository familyMemberRepository() {
        return familyMemberRepository;
    }

    public CustomerQuotaRepository customerQuotaRepository() {
        return customerQuotaRepository;
    }
}
