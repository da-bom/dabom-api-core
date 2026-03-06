package com.project.domain.mission.repository.projection;

import java.time.LocalDateTime;

import com.project.domain.mission.enums.MissionStatus;
import com.project.domain.mission.enums.RewardCategory;

public record MissionListRow(
        Long missionItemId,
        String missionText,
        MissionStatus status,
        Long rewardTemplateId,
        String rewardTemplateName,
        RewardCategory rewardCategory,
        String rewardUnit,
        Long rewardValue,
        LocalDateTime createdAt) {}
