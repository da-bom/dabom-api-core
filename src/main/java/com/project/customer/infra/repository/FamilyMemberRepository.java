package com.project.customer.infra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.project.customer.core.Role;
import com.project.family.infra.entity.FamilyMemberJpaEntity;

public interface FamilyMemberRepository extends JpaRepository<FamilyMemberJpaEntity, Long> {

    @Query("select f.role from FamilyMemberJpaEntity f where f.id = :id")
    Role findRoleById(Long id);
}
