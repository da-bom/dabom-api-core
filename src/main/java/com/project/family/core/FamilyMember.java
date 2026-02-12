package com.project.family.core;

import com.project.customer.core.Role;

import lombok.Builder;
import lombok.Getter;

/** 가족 그룹 구성원 도메인 객체 */
@Getter
@Builder
public class FamilyMember {
    private final Long id;
    private final Long familyId;
    private final Long customerId;
    private final Role role;

    public boolean isOwner() {
        return Role.OWNER.equals(this.role);
    }
}
