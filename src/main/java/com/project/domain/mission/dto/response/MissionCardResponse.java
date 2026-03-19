package com.project.domain.mission.dto.response;

import java.time.LocalDateTime;

import com.project.domain.mission.model.MissionListResult;
import com.project.domain.reward.dto.response.RewardResponse;

/** 미션 카드 응답 DTO */
public record MissionCardResponse(
        Long missionItemId,
        Long requestId,
        String missionText,
        String requestStatus,
        CustomerSimpleResponse target,
        CustomerSimpleResponse createdBy,
        RewardResponse reward,
        LocalDateTime createdAt) {
    public static MissionCardResponse from(MissionListResult.MissionCard card) {
        return new MissionCardResponse(
                card.missionItemId(),
                card.requestId(),
                card.missionText(),
                card.requestStatus(),
                CustomerSimpleResponse.from(card.target()),
                CustomerSimpleResponse.from(card.createdBy()),
                RewardResponse.from(card.reward()),
                card.createdAt());
    }
}
