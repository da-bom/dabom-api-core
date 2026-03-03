package com.project.domain.family.dto.response;

import java.time.LocalDateTime;

import com.project.domain.family.entity.Family;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "가족 이름 수정 응답")
public record FamilyNameUpdateResponse(
        @Schema(description = "가족 ID", example = "100") Long familyId,
        @Schema(description = "수정된 가족 이름", example = "김씨 가족") String name,
        @Schema(description = "수정 일시") LocalDateTime updatedAt) {

    public static FamilyNameUpdateResponse from(Family family) {
        return new FamilyNameUpdateResponse(family.getId(), family.getName(), family.getUpdatedAt());
    }
}
