package com.project.family.infra.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.family.infra.entity.FamilyMemberJpaEntity;

public interface JpaFamilyMemberRepository
        extends JpaRepository<FamilyMemberJpaEntity, Long> {
    List<FamilyMemberJpaEntity> findAllByFamilyId(Long familyId);
}
