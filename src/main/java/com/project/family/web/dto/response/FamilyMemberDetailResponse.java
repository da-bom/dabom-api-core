package com.project.family.web.dto.response;

import com.project.family.core.FamilyMemberRole;

public record FamilyMemberDetailResponse(
        Long customerId,
        String name,
        FamilyMemberRole role,
        Long monthlyLimitBytes,
        Long monthlyUsedBytes) {}
