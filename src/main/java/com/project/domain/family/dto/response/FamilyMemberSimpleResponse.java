package com.project.domain.family.dto.response;

import com.project.domain.family.model.FamilyMemberSummary;

public record FamilyMemberSimpleResponse(Long customerId, String name) {
    public static FamilyMemberSimpleResponse from(FamilyMemberSummary summary) {
        return new FamilyMemberSimpleResponse(summary.customerId(), summary.name());
    }
}
