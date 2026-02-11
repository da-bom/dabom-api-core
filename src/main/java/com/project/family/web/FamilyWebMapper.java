package com.project.family.web;

import com.project.family.core.Family;
import com.project.family.web.dto.response.FamilyDetailResponse;
import com.project.family.web.dto.response.FamilySearchResponse;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collections;

/**
 * Family Web 계층 변환 Mapper
 * - Request DTO -> Domain
 * - Domain -> Response DTO
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FamilyWebMapper {

    /** Domain -> FamilyDetailResponse */
    public static FamilyDetailResponse toDetailResponse(Family domain) {
        return new FamilyDetailResponse(
                domain.getId(),
                domain.getName(),
                domain.getCreatedById(),
                // FamilyMember 도메인 리스트를 DTO 리스트로 변환하는 로직이 필요할 수 있음
                // 현재는 QueryRepository에서 직접 Projection 하므로 예시로 둠
                Collections.emptyList(), 
                domain.getTotalQuotaBytes(),
                domain.getUsedBytes(),
                domain.calculateUsedPercent(),
                domain.getCurrentMonth(),
                null, // createdAt (필요 시 도메인에 추가)
                null  // updatedAt
        );
    }
}
