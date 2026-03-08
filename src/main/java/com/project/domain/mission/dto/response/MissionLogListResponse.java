package com.project.domain.mission.dto.response;

import java.util.List;

import com.project.domain.mission.model.MissionLogListResult;

/** 미션 로그 목록 응답 DTO */
public record MissionLogListResponse(
        List<MissionLogItemResponse> missions, String nextCursor, boolean hasNext) {
    public static MissionLogListResponse from(MissionLogListResult result) {
        return new MissionLogListResponse(
                result.missions().stream().map(MissionLogItemResponse::from).toList(),
                result.nextCursor(),
                result.hasNext());
    }
}
