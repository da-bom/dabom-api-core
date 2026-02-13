package com.project.policy.infra.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.policy.infra.entity.PolicyJpaEntity;

public interface PolicyJpaRepository extends JpaRepository<PolicyJpaEntity, Long> {}
