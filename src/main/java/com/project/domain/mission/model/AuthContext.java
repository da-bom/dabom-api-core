package com.project.domain.mission.model;

import com.project.domain.customer.enums.RoleType;

/** 인증된 사용자 컨텍스트를 서비스로 전달하는 도메인 모델이다. */
public record AuthContext(Long customerId, Long familyId, RoleType role) {
    /** OWNER 여부를 간단히 판별한다. */
    public boolean isOwner() {
        return RoleType.OWNER.equals(role);
    }
}
