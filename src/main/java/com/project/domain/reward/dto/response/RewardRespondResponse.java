package com.project.domain.reward.dto.response;

import java.time.LocalDateTime;

import com.project.domain.mission.dto.response.CustomerSimpleResponse;
import com.project.domain.mission.dto.response.MissionItemWithStatusResponse;
import com.project.domain.reward.model.RewardRespondResult;

/** 보상 요청 응답 처리 결과 DTO */
public record RewardRespondResponse(
        Long requestId,
        String status,
        MissionItemWithStatusResponse missionItem,
        CustomerSimpleResponse respondedBy,
        String rejectReason,
        LocalDateTime updatedAt) {
    public static RewardRespondResponse from(RewardRespondResult result) {
        return new RewardRespondResponse(
                result.requestId(),
                result.status(),
                MissionItemWithStatusResponse.from(result.missionItem()),
                CustomerSimpleResponse.from(result.respondedBy()),
                result.rejectReason(),
                result.updatedAt());
    }
}
