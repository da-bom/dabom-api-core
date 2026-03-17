package com.project.common.fixtures;

import com.project.common.auth.enums.RoleType;
import com.project.domain.family.entity.FamilyMember;

public class FamilyMemberFixtures {

    public static final Long FAMILY_ID = 100L;

    public static FamilyMember owner(Long familyId, Long customerId) {
        return FamilyMember.builder()
                .familyId(familyId)
                .customerId(customerId)
                .role(RoleType.OWNER)
                .build();
    }

    public static FamilyMember member(Long familyId, Long customerId) {
        return FamilyMember.builder()
                .familyId(familyId)
                .customerId(customerId)
                .role(RoleType.MEMBER)
                .build();
    }
}
