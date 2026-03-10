package com.project.domain.reward.dto.request;

import jakarta.validation.constraints.NotNull;

import com.project.domain.reward.enums.RewardCategory;

public class RewardTemplateRequest {

    public record Upsert(
            @NotNull String name,
            @NotNull RewardCategory category,
            @NotNull Long defaultValue,
            @NotNull String unit,
            @NotNull Boolean isSystem) {}
}
