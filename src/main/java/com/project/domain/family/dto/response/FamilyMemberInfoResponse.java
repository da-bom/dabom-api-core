package com.project.domain.family.dto.response;

import com.project.common.auth.enums.RoleType;
import com.project.domain.family.model.FamilyMemberInfo;

public record FamilyMemberInfoResponse(Long customerId, String name, RoleType role) {
    public static FamilyMemberInfoResponse from(FamilyMemberInfo info) {
        return new FamilyMemberInfoResponse(info.customerId(), info.name(), info.role());
    }
}
