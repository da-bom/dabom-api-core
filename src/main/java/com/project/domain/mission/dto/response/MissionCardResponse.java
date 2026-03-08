package com.project.domain.mission.dto.response;

import java.time.LocalDateTime;

import com.project.domain.mission.model.MissionListResult;

/** 미션 카드 응답 DTO다. */
public record MissionCardResponse(
        Long missionItemId,
        String missionText,
        String requestStatus,
        CustomerSimpleResponse target,
        CustomerSimpleResponse createdBy,
        RewardTemplateSimpleResponse rewardTemplate,
        Long rewardValue,
        LocalDateTime createdAt) {
    public static MissionCardResponse from(MissionListResult.MissionCard card) {
        return new MissionCardResponse(
                card.missionItemId(),
                card.missionText(),
                card.requestStatus(),
                CustomerSimpleResponse.from(card.target()),
                CustomerSimpleResponse.from(card.createdBy()),
                RewardTemplateSimpleResponse.from(card.rewardTemplate()),
                card.rewardValue(),
                card.createdAt());
    }
}
