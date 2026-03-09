package com.project.domain.mission.model;

import java.time.LocalDateTime;

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
