package com.project.family.infra.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.family.infra.entity.FamilyJpaEntity;

public interface JpaFamilyRepository extends JpaRepository<FamilyJpaEntity, Long> {}
