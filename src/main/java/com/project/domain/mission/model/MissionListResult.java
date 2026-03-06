package com.project.domain.mission.model;

import java.time.LocalDateTime;
import java.util.List;

import com.project.domain.mission.enums.MissionStatus;
import com.project.domain.mission.enums.RewardCategory;

public record MissionListResult(List<MissionDetail> missions) {

    public record MissionDetail(
            Long missionItemId,
            String missionText,
            MissionStatus status,
            RewardTemplateDetail rewardTemplate,
            Long rewardValue,
            LocalDateTime createdAt) {}

    public record RewardTemplateDetail(
            Long id, String name, RewardCategory category, String unit) {}
}
