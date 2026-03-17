package com.project.domain.family.model;

import com.project.common.auth.enums.RoleType;

public record FamilyMemberDetail(
        Long customerId,
        String name,
        RoleType role,
        Long monthlyLimitBytes,
        Long monthlyUsedBytes) {}
