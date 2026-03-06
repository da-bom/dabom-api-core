package com.project.domain.mission.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.domain.mission.entity.MissionItem;

public interface MissionItemRepository
        extends JpaRepository<MissionItem, Long>, MissionItemRepositoryCustom {}
