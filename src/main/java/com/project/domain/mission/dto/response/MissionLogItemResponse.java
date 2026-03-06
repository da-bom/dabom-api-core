package com.project.domain.mission.dto.response;

import java.time.LocalDateTime;

import com.project.domain.mission.model.MissionLogListResult;

/** 미션 요청 로그 단건 응답 DTO다. */
public record MissionLogItemResponse(
        Long requestId,
        String status,
        MissionItemSimpleResponse missionItem,
        CustomerSimpleResponse assignedTo,
        CustomerSimpleResponse requestedBy,
        CustomerSimpleResponse respondedBy,
        String rejectReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static MissionLogItemResponse from(MissionLogListResult.MissionLogItem item) {
        return new MissionLogItemResponse(
                item.requestId(),
                item.status(),
                MissionItemSimpleResponse.from(item.missionItem()),
                CustomerSimpleResponse.from(item.assignedTo()),
                CustomerSimpleResponse.from(item.requestedBy()),
                CustomerSimpleResponse.from(item.respondedBy()),
                item.rejectReason(),
                item.createdAt(),
                item.updatedAt());
    }
}
