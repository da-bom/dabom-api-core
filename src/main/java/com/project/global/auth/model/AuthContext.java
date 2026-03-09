package com.project.global.auth.model;

import com.project.domain.customer.enums.RoleType;

public record AuthContext(Long customerId, Long familyId, RoleType role) {
    public boolean isOwner() {
        return RoleType.OWNER.equals(role);
    }
}
