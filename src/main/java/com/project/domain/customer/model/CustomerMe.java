package com.project.domain.customer.model;

import com.project.domain.customer.enums.RoleType;

public record CustomerMe(
        Long customerId, String name, String phoneNumber, Long familyId, RoleType role) {}
