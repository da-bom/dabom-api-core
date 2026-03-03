package com.project.domain.usagerecord.dto.response;

import java.util.List;

import com.project.domain.usagerecord.model.FamilyCustomersUsageSummary;

// FamilyController.getCustomersUsage에서 반환할 때 FamilyCustomersUsageSummary 모델을 API 응답으로 변환하는 DTO
public record FamilyCustomersUsageSummaryResponse(
        Long familyId, int year, int month, List<CustomerUsageResponse> customers) {
    public static FamilyCustomersUsageSummaryResponse from(
            FamilyCustomersUsageSummary familyCustomersUsageSummary) {
        List<CustomerUsageResponse> customers =
                familyCustomersUsageSummary.customers().stream()
                        .map(CustomerUsageResponse::from)
                        .toList();
        return new FamilyCustomersUsageSummaryResponse(
                familyCustomersUsageSummary.familyId(),
                familyCustomersUsageSummary.year(),
                familyCustomersUsageSummary.month(),
                customers);
    }
}
