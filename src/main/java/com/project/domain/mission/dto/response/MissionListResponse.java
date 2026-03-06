package com.project.domain.mission.dto.response;

import java.util.List;

import com.project.domain.mission.model.MissionListResult;

/** 미션 목록 응답 DTO다. */
public record MissionListResponse(
        List<MissionCardResponse> content, String nextCursor, boolean hasNext) {
    public static MissionListResponse from(MissionListResult result) {
        return new MissionListResponse(
                result.content().stream().map(MissionCardResponse::from).toList(),
                result.nextCursor(),
                result.hasNext());
    }
}
