package com.project.domain.reward.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.project.domain.reward.enums.RewardCategory;

import io.swagger.v3.oas.annotations.media.Schema;

public class RewardTemplateRequest {

    @Schema(name = "RewardTemplateCreateRequest")
    public record Create(
            @NotBlank @Size(max = 100) String name,
            @NotNull RewardCategory category,
            String thumbnailUrl,
            @NotNull @Positive Integer price) {}

    @Schema(name = "RewardTemplateUpdateRequest")
    public record Update(
            @NotBlank @Size(max = 100) String name,
            String thumbnailUrl,
            @NotNull @Positive Integer price,
            @NotNull Boolean isActive) {}
}
