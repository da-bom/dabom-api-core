package com.project.family.web;

import java.util.Collections;

import com.project.family.core.Family;
import com.project.family.web.dto.response.FamilyDetailResponse;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Family Web 계층 변환 Mapper - Request DTO -> Domain - Domain -> Response DTO */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FamilyWebMapper {

    /** Domain -> FamilyDetailResponse */
    public static FamilyDetailResponse toDetailResponse(Family domain) {
        return new FamilyDetailResponse(
                domain.getId(),
                domain.getName(),
                domain.getCreatedById(),
                Collections.emptyList(),
                domain.getTotalQuotaBytes(),
                domain.getUsedBytes(),
                domain.calculateUsedPercent(),
                domain.getCurrentMonth(),
                null, // createdAt
                null // updatedAt
                );
    }
}
