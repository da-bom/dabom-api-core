package com.project.domain.reward.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.domain.reward.entity.RewardTemplate;

/** Reward DB 접근 전용 Repository */
public interface RewardTemplateRepository extends JpaRepository<RewardTemplate, Long> {}
