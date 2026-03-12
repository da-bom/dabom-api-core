package com.project.domain.reward.dto.response;

import com.project.domain.mission.model.MissionListResult;

/** 보상 정보 응답 DTO */
public record RewardResponse(
        Long rewardId, String name, String category, String thumbnailUrl, Long templateId) {
    public static RewardResponse from(MissionListResult.Reward reward) {
        return new RewardResponse(
                reward.rewardId(),
                reward.name(),
                reward.category().name(),
                reward.thumbnailUrl(),
                reward.templateId());
    }
}
