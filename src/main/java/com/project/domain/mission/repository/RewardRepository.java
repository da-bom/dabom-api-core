package com.project.domain.mission.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.domain.mission.entity.Reward;

public interface RewardRepository extends JpaRepository<Reward, Long> {}
