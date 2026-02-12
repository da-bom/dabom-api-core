package com.project.family.infra.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.project.customer.core.Role;
import com.project.family.infra.entity.FamilyMemberJpaEntity;

public interface JpaFamilyMemberRepository extends JpaRepository<FamilyMemberJpaEntity, Long> {
    List<FamilyMemberJpaEntity> findAllByFamilyId(Long familyId);

    @Query("select f.role from FamilyMemberJpaEntity f where f.id = :id")
    Role findRoleById(Long id);
}
