package com.project.domain.family.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.project.domain.family.model.FamilySearchResult;

public record FamilySearchResponse(
        Long familyId,
        String familyName,
        List<FamilyMemberSimpleResponse> customers,
        LocalDateTime createdAt) {
    public static FamilySearchResponse from(FamilySearchResult result) {
        List<FamilyMemberSimpleResponse> customers =
                result.customers().stream().map(FamilyMemberSimpleResponse::from).toList();

        return new FamilySearchResponse(
                result.familyId(), result.familyName(), customers, result.createdAt());
    }
}
