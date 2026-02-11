package com.project.family.core;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

/** 가족 그룹 구성원 도메인 객체 */
@Getter
@Builder
public class FamilyMember {
    private final Long id;
    private final Long familyId;
    private final Long customerId;
    private final FamilyMemberRole role;

    public boolean isOwner() {
        return FamilyMemberRole.OWNER.equals(this.role);
    }
}
