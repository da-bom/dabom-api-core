package com.project.domain.mission.dto.response;

import com.project.domain.mission.model.MissionLogListResult;
import com.project.domain.reward.dto.response.RewardResponse;

/** 미션 요약 응답 DTO */
public record MissionItemSimpleResponse(
        Long missionItemId, String missionText, RewardResponse reward) {
    public static MissionItemSimpleResponse from(MissionLogListResult.MissionItemSimple item) {
        return new MissionItemSimpleResponse(
                item.missionItemId(), item.missionText(), RewardResponse.from(item.reward()));
    }
}
