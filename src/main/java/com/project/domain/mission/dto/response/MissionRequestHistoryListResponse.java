package com.project.domain.mission.dto.response;

import java.util.List;

import com.project.domain.mission.model.MissionRequestHistoryListResult;

public record MissionRequestHistoryListResponse(
        List<MissionRequestHistoryItemResponse> requests, String nextCursor, boolean hasNext) {
    public static MissionRequestHistoryListResponse from(MissionRequestHistoryListResult result) {
        return new MissionRequestHistoryListResponse(
                result.requests().stream().map(MissionRequestHistoryItemResponse::from).toList(),
                result.nextCursor(),
                result.hasNext());
    }
}
