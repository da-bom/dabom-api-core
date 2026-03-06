package com.project.domain.mission.model;

import java.time.LocalDateTime;

/** 미션 완료 요청 생성 결과 모델이다. */
public record MissionRequestResult(
        Long requestId,
        MissionLogListResult.MissionItemSimple missionItem,
        String status,
        MissionListResult.CustomerSummary requestedBy,
        LocalDateTime createdAt) {}
