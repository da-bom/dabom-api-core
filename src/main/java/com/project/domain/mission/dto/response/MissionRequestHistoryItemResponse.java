package com.project.domain.mission.dto.response;

import java.time.LocalDateTime;

import com.project.domain.mission.model.MissionRequestHistoryListResult;

/** 미션 완료 요청 이력 응답 DTO. status는 MissionRequest.status 값(PENDING, APPROVED, REJECTED)을 그대로 사용한다. */
public record MissionRequestHistoryItemResponse(
        Long requestId,
        String status,
        String rejectReason,
        MissionItemSimpleResponse missionItem,
        CustomerSimpleResponse requestedBy,
        CustomerSimpleResponse respondedBy,
        LocalDateTime requestedAt,
        LocalDateTime respondedAt) {
    public static MissionRequestHistoryItemResponse from(
            MissionRequestHistoryListResult.MissionRequestHistoryItem item) {
        return new MissionRequestHistoryItemResponse(
                item.requestId(),
                item.status(),
                item.rejectReason(),
                MissionItemSimpleResponse.from(item.missionItem()),
                CustomerSimpleResponse.from(item.requestedBy()),
                CustomerSimpleResponse.from(item.respondedBy()),
                item.requestedAt(),
                item.respondedAt());
    }
}
