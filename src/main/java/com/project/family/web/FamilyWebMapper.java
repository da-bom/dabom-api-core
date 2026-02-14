package com.project.family.web;

import java.util.Collections;

import org.springframework.stereotype.Component;

import com.project.family.core.Family;
import com.project.family.web.dto.response.FamilyDetailResponse;
import com.project.family.web.dto.response.FamilyMemberSimpleResponse;
import com.project.family.web.dto.response.FamilySearchResponse;

/** Family Web 계층 변환 Mapper - Request DTO -> Domain - Domain -> Response DTO */
@Component
public class FamilyWebMapper {

    /** Domain -> FamilySearchResponse */
    public static FamilySearchResponse toResponse(Family family) {
        return new FamilySearchResponse(
                family.getId(),
                family.getName(),
                family.getMembers().stream()
                        .map(m -> new FamilyMemberSimpleResponse(m.getCustomerId(), null))
                        .toList(),
                null // createdAt (도메인 엔티티에 필드 추가 필요 시 반영)
                );
    }

    /** Domain -> FamilyDetailResponse */
    public static FamilyDetailResponse toDetailResponse(Family family) {
        return new FamilyDetailResponse(
                family.getId(),
                family.getName(),
                family.getCreatedById(),
                Collections.emptyList(),
                family.getTotalQuotaBytes(),
                family.getUsedBytes(),
                family.calculateUsedPercent(),
                family.getCurrentMonth(),
                null, // createdAt
                null // updatedAt
                );
    }
}
