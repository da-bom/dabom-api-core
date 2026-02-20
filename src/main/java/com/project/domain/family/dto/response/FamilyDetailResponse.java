package com.project.domain.family.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.project.domain.family.model.FamilyDetail;

public record FamilyDetailResponse(
        Long familyId,
        String familyName,
        Long createdById,
        List<FamilyMemberDetailResponse> customers,
        Long totalQuotaBytes,
        Long usedBytes,
        double usedPercent,
        LocalDate currentMonth,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static FamilyDetailResponse from(FamilyDetail detail) {
        List<FamilyMemberDetailResponse> customers =
                detail.customers().stream().map(FamilyMemberDetailResponse::from).toList();

        return new FamilyDetailResponse(
                detail.familyId(),
                detail.familyName(),
                detail.createdById(),
                customers,
                detail.totalQuotaBytes(),
                detail.usedBytes(),
                detail.usedPercent(),
                detail.currentMonth(),
                detail.createdAt(),
                detail.updatedAt());
    }
}
