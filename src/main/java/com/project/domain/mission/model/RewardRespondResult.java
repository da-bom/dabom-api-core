package com.project.domain.mission.model;

import java.time.LocalDateTime;

/** 보상 요청 응답 처리 결과 모델이다. */
public record RewardRespondResult(
        Long requestId,
        String status,
        MissionItemWithStatus missionItem,
        MissionListResult.CustomerSummary respondedBy,
        String rejectReason,
        LocalDateTime updatedAt) {

    /** 상태 포함 미션 요약 모델이다. */
    public record MissionItemWithStatus(
            Long missionItemId,
            String missionText,
            String status,
            Long rewardValue,
            MissionListResult.RewardTemplate rewardTemplate) {}
}
