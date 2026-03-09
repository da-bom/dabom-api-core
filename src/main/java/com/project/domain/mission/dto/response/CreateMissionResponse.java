package com.project.domain.mission.dto.response;

import java.time.LocalDateTime;

import com.project.domain.mission.model.CreateMissionResult;

/** 미션 생성 응답 DTO */
public record CreateMissionResponse(Long missionItemId, LocalDateTime createdAt) {
    public static CreateMissionResponse from(CreateMissionResult result) {
        return new CreateMissionResponse(result.missionItemId(), result.createdAt());
    }
}
