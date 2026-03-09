package com.project.domain.mission.dto.response;

import com.project.domain.mission.model.MissionListResult;

public record RewardResponse(
        Long rewardId, String name, String category, Long value, String unit, Long templateId) {
    public static RewardResponse from(MissionListResult.Reward reward) {
        return new RewardResponse(
                reward.rewardId(),
                reward.name(),
                reward.category().name(),
                reward.value(),
                reward.unit(),
                reward.templateId());
    }
}
