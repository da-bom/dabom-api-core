package com.project.family.infra.mapper;

import com.project.family.core.Family;
import com.project.family.infra.entity.FamilyJpaEntity;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Family 계층 간 변환 Mapper
 * - static 메서드로 구성하여 상태를 가지지 않음
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FamilyEntityMapper {

    /** Domain -> JpaEntity */
    public static FamilyJpaEntity toEntity(Family domain) {
        return FamilyJpaEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .createdById(domain.getCreatedById())
                .totalQuotaBytes(domain.getTotalQuotaBytes())
                .usedBytes(domain.getUsedBytes())
                .currentMonth(domain.getCurrentMonth())
                .build();
    }

    /** JpaEntity -> Domain */
    public static Family toDomain(FamilyJpaEntity entity) {
        return Family.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createdById(entity.getCreatedById())
                .totalQuotaBytes(entity.getTotalQuotaBytes())
                .usedBytes(entity.getUsedBytes())
                .currentMonth(entity.getCurrentMonth())
                .build();
    }
}
