package com.project.domain.mission.dto.response;

import com.project.domain.mission.model.RewardRespondResult;

/** 상태 포함 미션 요약 응답 DTO다. */
public record MissionItemWithStatusResponse(
        Long missionItemId,
        String missionText,
        String status,
        Long rewardValue,
        RewardTemplateSimpleResponse rewardTemplate) {
    public static MissionItemWithStatusResponse from(
            RewardRespondResult.MissionItemWithStatus item) {
        return new MissionItemWithStatusResponse(
                item.missionItemId(),
                item.missionText(),
                item.status(),
                item.rewardValue(),
                RewardTemplateSimpleResponse.from(item.rewardTemplate()));
    }
}
