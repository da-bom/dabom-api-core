package com.project.family.infra.mapper;

import com.project.family.core.FamilyMember;
import com.project.family.infra.entity.FamilyMemberJpaEntity;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** FamilyMember 계층 간 변환 Mapper - static 메서드로 구성하여 상태를 가지지 않음 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FamilyMemberEntityMapper {

    /** Member Domain -> JpaEntity */
    public static FamilyMemberJpaEntity toEntity(FamilyMember domain) {
        return FamilyMemberJpaEntity.builder()
                .id(domain.getId())
                .familyId(domain.getFamilyId())
                .customerId(domain.getCustomerId())
                .role(domain.getRole())
                .build();
    }

    /** Member JpaEntity -> Domain */
    public static FamilyMember toDomain(FamilyMemberJpaEntity entity) {
        return FamilyMember.builder()
                .id(entity.getId())
                .familyId(entity.getFamilyId())
                .customerId(entity.getCustomerId())
                .role(entity.getRole())
                .build();
    }
}
