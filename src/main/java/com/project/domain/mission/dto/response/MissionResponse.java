package com.project.domain.mission.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.project.domain.mission.enums.MissionStatus;
import com.project.domain.mission.model.MissionListResult;

public class MissionResponse {

    public record ListResult(List<MissionItemResult> missions) {
        public static ListResult from(MissionListResult result) {
            return new ListResult(result.missions().stream().map(MissionItemResult::from).toList());
        }
    }

    public record MissionItemResult(
            Long missionItemId,
            String missionText,
            MissionStatus status,
            RewardTemplateResult rewardTemplate,
            Long rewardValue,
            LocalDateTime createdAt) {
        public static MissionItemResult from(MissionListResult.MissionDetail detail) {
            return new MissionItemResult(
                    detail.missionItemId(),
                    detail.missionText(),
                    detail.status(),
                    RewardTemplateResult.from(detail.rewardTemplate()),
                    detail.rewardValue(),
                    detail.createdAt());
        }
    }

    public record RewardTemplateResult(Long id, String name, String category, String unit) {
        public static RewardTemplateResult from(MissionListResult.RewardTemplateDetail rewardTemplate) {
            return new RewardTemplateResult(
                    rewardTemplate.id(),
                    rewardTemplate.name(),
                    rewardTemplate.category().name(),
                    rewardTemplate.unit());
        }
    }
}
