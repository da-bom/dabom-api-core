package com.project.domain.family.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "가족 구성원 권한/한도 수정 결과")
public record AdminFamilyUpdateResponse(
        @Schema(description = "가족 ID") Long familyId,
        @Schema(description = "수정된 구성원 수") int updatedCount) {}
