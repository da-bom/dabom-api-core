package com.project.domain.mission.model;

import java.time.LocalDateTime;
import java.util.List;

/** Mission 로그 목록 조회 결과 모델이다. */
public record MissionLogListResult(
        List<MissionLogItem> missions, String nextCursor, boolean hasNext) {

    /** Mission 로그 단건 모델이다. */
    public record MissionLogItem(
            Long logId,
            String actionType,
            String message,
            MissionItemSimple missionItem,
            MissionListResult.CustomerSummary assignedTo,
            MissionListResult.CustomerSummary actor,
            LocalDateTime createdAt) {}

    /** 응답용 미션 요약 모델이다. */
    public record MissionItemSimple(
            Long missionItemId,
            String missionText,
            Long rewardValue,
            MissionListResult.RewardTemplate rewardTemplate) {}
}
