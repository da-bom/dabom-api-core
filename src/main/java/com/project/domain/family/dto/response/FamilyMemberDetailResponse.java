package com.project.domain.family.dto.response;

import com.project.domain.customer.enums.RoleType;

public record FamilyMemberDetailResponse(
        Long customerId,
        String name,
        RoleType role,
        Long monthlyLimitBytes,
        Long monthlyUsedBytes) {}
