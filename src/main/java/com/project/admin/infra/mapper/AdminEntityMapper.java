package com.project.admin.infra.mapper;

import org.springframework.stereotype.Component;

import com.project.admin.core.Admin;
import com.project.admin.infra.entity.AdminJpaEntity;

/** Admin 도메인과 JPA Entity 변환기 */
@Component
public class AdminEntityMapper {

    public Admin toDomain(AdminJpaEntity entity) {
        return Admin.withId(entity.getId(), entity.getEmail(), entity.getName(), entity.getPasswordHash());
    }

    public AdminJpaEntity toEntity(Admin admin) {
        return AdminJpaEntity.builder()
                .id(admin.getAdminId())
                .email(admin.getEmail())
                .name(admin.getName())
                .passwordHash(admin.getPasswordHash())
                .build();
    }
}
