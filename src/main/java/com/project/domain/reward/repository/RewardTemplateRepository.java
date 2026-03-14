package com.project.domain.reward.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.domain.reward.entity.RewardTemplate;
import com.project.domain.reward.enums.RewardCategory;

public interface RewardTemplateRepository extends JpaRepository<RewardTemplate, Long> {
    List<RewardTemplate> findAllByDeletedAtIsNull();

    List<RewardTemplate> findAllByDeletedAtIsNullAndIsActiveTrueAndCategoryOrderByIdAsc(
            RewardCategory category);

    Optional<RewardTemplate> findByIdAndDeletedAtIsNull(Long id);
}
