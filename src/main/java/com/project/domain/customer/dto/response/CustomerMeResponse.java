package com.project.domain.customer.dto.response;

import com.project.domain.customer.enums.RoleType;
import com.project.domain.customer.model.CustomerMe;

public record CustomerMeResponse(
        Long customerId, String name, String phoneNumber, Long familyId, RoleType role) {

    public static CustomerMeResponse from(CustomerMe customerMe) {
        return new CustomerMeResponse(
                customerMe.customerId(),
                customerMe.name(),
                customerMe.phoneNumber(),
                customerMe.familyId(),
                customerMe.role());
    }
}
