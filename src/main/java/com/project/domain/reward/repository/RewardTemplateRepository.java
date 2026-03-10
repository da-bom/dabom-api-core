package com.project.domain.reward.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.domain.reward.entity.RewardTemplate;

public interface RewardTemplateRepository extends JpaRepository<RewardTemplate, Long> {
    List<RewardTemplate> findAllByDeletedAtIsNull();

    Optional<RewardTemplate> findByIdAndDeletedAtIsNull(Long id);
}
