package com.project.family.web.dto.response;

import com.project.customer.core.Role;

public record FamilyMemberDetailResponse(
        Long customerId, String name, Role role, Long monthlyLimitBytes, Long monthlyUsedBytes) {}
