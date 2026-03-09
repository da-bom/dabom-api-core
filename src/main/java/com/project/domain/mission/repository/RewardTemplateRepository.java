package com.project.domain.mission.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.domain.mission.entity.RewardTemplate;

/** 미션 보상 템플릿 조회를 담당하는 Repository다. */
public interface RewardTemplateRepository extends JpaRepository<RewardTemplate, Long> {}
