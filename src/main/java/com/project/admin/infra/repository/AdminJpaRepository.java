package com.project.admin.infra.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.admin.infra.entity.AdminJpaEntity;

public interface AdminJpaRepository extends JpaRepository<AdminJpaEntity, Long> {

    AdminJpaEntity findByEmail(String email);
}
