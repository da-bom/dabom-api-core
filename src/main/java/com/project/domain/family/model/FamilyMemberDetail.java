package com.project.domain.family.model;

import com.project.domain.customer.enums.RoleType;

public record FamilyMemberDetail(
        Long customerId,
        String name,
        RoleType role,
        Long monthlyLimitBytes,
        Long monthlyUsedBytes) {}
