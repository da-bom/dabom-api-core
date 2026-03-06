package com.project.domain.mission.model;

import java.time.LocalDateTime;

/** 미션 생성 결과 모델이다. */
public record CreateMissionResult(Long missionItemId, LocalDateTime createdAt) {}
