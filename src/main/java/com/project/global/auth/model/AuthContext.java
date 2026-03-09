package com.project.global.auth.model;

import com.project.domain.customer.enums.RoleType;

/** 인증된 사용자의 ID, 가족 ID, 역할을 담는 컨텍스트 모델입니다. 여러 도메인 서비스에서 공통으로 사용됩니다. */
public record AuthContext(Long customerId, Long familyId, RoleType role) {
    public boolean isOwner() {
        return RoleType.OWNER.equals(role);
    }
}
