package com.project.domain.mission.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 미션 생성 요청 DTO */
public record CreateMissionRequest(
        @NotBlank String missionText,
        @NotNull @Positive Long targetCustomerId,
        @NotNull @Positive Long rewardTemplateId,
        @NotNull @Positive Long rewardValue) {}
