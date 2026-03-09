package com.project.domain.mission.dto.response;

import com.project.domain.reward.dto.response.RewardResponse;
import com.project.domain.reward.model.RewardRespondResult;

/** 상태 포함 미션 요약 응답 DTO */
public record MissionItemWithStatusResponse(
        Long missionItemId, String missionText, String status, RewardResponse reward) {
    public static MissionItemWithStatusResponse from(
            RewardRespondResult.MissionItemWithStatus item) {
        return new MissionItemWithStatusResponse(
                item.missionItemId(),
                item.missionText(),
                item.status(),
                RewardResponse.from(item.reward()));
    }
}
