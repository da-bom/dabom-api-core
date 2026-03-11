package com.project.domain.reward.dto.request;

import jakarta.validation.constraints.NotNull;

import com.project.domain.reward.enums.RewardCategory;

public class RewardTemplateRequest {

    public record Create(
            @NotNull String name,
            @NotNull RewardCategory category,
            String thumbnailUrl,
            @NotNull Integer price) {}

    public record Update(
            @NotNull String name,
            String thumbnailUrl,
            @NotNull Integer price,
            @NotNull Boolean isActive) {}
}
