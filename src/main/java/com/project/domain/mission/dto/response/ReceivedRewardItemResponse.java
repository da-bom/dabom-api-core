package com.project.domain.mission.dto.response;

import java.time.LocalDateTime;

import com.project.domain.mission.model.ReceivedRewardListResult;

/** 보상 수령 내역 단건 응답 DTO다. */
public record ReceivedRewardItemResponse(
        Long requestId,
        MissionItemSimpleResponse missionItem,
        CustomerSimpleResponse approvedBy,
        LocalDateTime approvedAt) {
    public static ReceivedRewardItemResponse from(ReceivedRewardListResult.ReceivedRewardItem item) {
        return new ReceivedRewardItemResponse(
                item.requestId(),
                MissionItemSimpleResponse.from(item.missionItem()),
                CustomerSimpleResponse.from(item.approvedBy()),
                item.approvedAt());
    }
}
