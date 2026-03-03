package com.project.domain.family.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "가족 이름 수정 요청")
public record FamilyNameUpdateRequest(
        @Schema(
                        description = "수정할 가족 이름",
                        example = "김씨 가족",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank
                @Size(max = 100)
                String name) {}
