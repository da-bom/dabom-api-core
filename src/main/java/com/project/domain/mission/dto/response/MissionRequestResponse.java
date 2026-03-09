package com.project.domain.mission.dto.response;

import java.time.LocalDateTime;

import com.project.domain.mission.model.MissionRequestResult;

/** 미션 완료 요청 응답 DTO */
public record MissionRequestResponse(
        Long requestId,
        MissionItemSimpleResponse missionItem,
        String status,
        CustomerSimpleResponse requestedBy,
        LocalDateTime createdAt) {
    public static MissionRequestResponse from(MissionRequestResult result) {
        return new MissionRequestResponse(
                result.requestId(),
                MissionItemSimpleResponse.from(result.missionItem()),
                result.status(),
                CustomerSimpleResponse.from(result.requestedBy()),
                result.createdAt());
    }
}
