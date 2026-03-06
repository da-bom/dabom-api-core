package com.project.domain.mission.dto.response;

import com.project.domain.mission.model.MissionListResult;

/** 보상 템플릿 요약 응답 DTO다. */
public record RewardTemplateSimpleResponse(Long id, String name, String category, String unit) {
    public static RewardTemplateSimpleResponse from(MissionListResult.RewardTemplate template) {
        return new RewardTemplateSimpleResponse(
                template.id(), template.name(), template.category().name(), template.unit());
    }
}
