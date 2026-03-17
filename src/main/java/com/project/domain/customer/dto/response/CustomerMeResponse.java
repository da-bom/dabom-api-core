package com.project.domain.customer.dto.response;

import com.project.domain.customer.entity.Customer;
import com.project.domain.customer.enums.RoleType;
import com.project.domain.family.entity.FamilyMember;

public record CustomerMeResponse(
        Long customerId, String name, String phoneNumber, Long familyId, RoleType role) {

    public static CustomerMeResponse of(Customer customer, FamilyMember familyMember) {
        return new CustomerMeResponse(
                customer.getId(),
                customer.getName(),
                customer.getPhoneNumber(),
                familyMember.getFamilyId(),
                familyMember.getRole());
    }
}
