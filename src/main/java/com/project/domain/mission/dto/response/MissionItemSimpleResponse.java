package com.project.domain.mission.dto.response;

import com.project.domain.mission.model.MissionLogListResult;

/** 미션 요약 응답 DTO다. */
public record MissionItemSimpleResponse(
        Long missionItemId,
        String missionText,
        Long rewardValue,
        RewardTemplateSimpleResponse rewardTemplate) {
    public static MissionItemSimpleResponse from(MissionLogListResult.MissionItemSimple item) {
        return new MissionItemSimpleResponse(
                item.missionItemId(),
                item.missionText(),
                item.rewardValue(),
                RewardTemplateSimpleResponse.from(item.rewardTemplate()));
    }
}
