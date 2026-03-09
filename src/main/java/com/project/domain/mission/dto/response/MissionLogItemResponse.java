package com.project.domain.mission.dto.response;

import java.time.LocalDateTime;

import com.project.domain.mission.model.MissionLogListResult;

/** Mission 로그 단건 응답 DTO */
public record MissionLogItemResponse(
        Long logId,
        String actionType,
        String message,
        MissionItemSimpleResponse missionItem,
        CustomerSimpleResponse assignedTo,
        CustomerSimpleResponse actor,
        LocalDateTime createdAt) {
    public static MissionLogItemResponse from(MissionLogListResult.MissionLogItem item) {
        return new MissionLogItemResponse(
                item.logId(),
                item.actionType(),
                item.message(),
                MissionItemSimpleResponse.from(item.missionItem()),
                CustomerSimpleResponse.from(item.assignedTo()),
                CustomerSimpleResponse.from(item.actor()),
                item.createdAt());
    }
}
