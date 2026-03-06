package com.project.domain.mission.model;

import java.time.LocalDateTime;
import java.util.List;

/** 미션 요청 로그 목록 조회 결과 모델이다. */
public record MissionLogListResult(
        List<MissionLogItem> missions, String nextCursor, boolean hasNext) {

    /** 미션 요청 이력 단건 모델이다. */
    public record MissionLogItem(
            Long requestId,
            String status,
            MissionItemSimple missionItem,
            MissionListResult.CustomerSummary assignedTo,
            MissionListResult.CustomerSummary requestedBy,
            MissionListResult.CustomerSummary respondedBy,
            String rejectReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {}

    /** 응답 내 미션 요약 모델이다. */
    public record MissionItemSimple(
            Long missionItemId,
            String missionText,
            Long rewardValue,
            MissionListResult.RewardTemplate rewardTemplate) {}
}
