package com.project.domain.family.dto.response;

import com.project.domain.customer.enums.RoleType;
import com.project.domain.family.model.FamilyMemberDetail;

public record FamilyMemberDetailResponse(
        Long customerId,
        String name,
        RoleType role,
        Long monthlyLimitBytes,
        Long monthlyUsedBytes) {
    public static FamilyMemberDetailResponse from(FamilyMemberDetail detail) {
        return new FamilyMemberDetailResponse(
                detail.customerId(),
                detail.name(),
                detail.role(),
                detail.monthlyLimitBytes(),
                detail.monthlyUsedBytes());
    }
}
