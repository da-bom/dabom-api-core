package com.project.domain.customer.model;

import com.project.common.auth.enums.RoleType;

public record CustomerMe(
        Long customerId, String name, String phoneNumber, Long familyId, RoleType role) {}
