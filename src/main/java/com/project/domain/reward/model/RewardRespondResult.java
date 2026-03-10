package com.project.domain.reward.model;

import java.time.LocalDateTime;

import com.project.domain.mission.model.MissionListResult;

public record RewardRespondResult(
        Long requestId,
        String status,
        MissionItemWithStatus missionItem,
        MissionListResult.CustomerSummary respondedBy,
        String rejectReason,
        LocalDateTime updatedAt) {

    public record MissionItemWithStatus(
            Long missionItemId,
            String missionText,
            String status,
            MissionListResult.Reward reward) {}
}
