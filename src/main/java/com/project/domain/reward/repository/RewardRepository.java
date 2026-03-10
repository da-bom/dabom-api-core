package com.project.domain.reward.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.domain.reward.entity.Reward;

public interface RewardRepository extends JpaRepository<Reward, Long> {}
