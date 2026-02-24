package com.project.domain.usagerecord.dto.response;

import java.util.List;

import com.project.domain.usagerecord.model.FamilyCustomersUsage;

// 하단 화면 dto
public record FamilyCustomersUsageResponse(
        Long familyId, int year, int month, List<CustomerUsageResponse> customers) {
    public static FamilyCustomersUsageResponse from(FamilyCustomersUsage familyCustomersUsage) {
        List<CustomerUsageResponse> customers =
                familyCustomersUsage.customers().stream().map(CustomerUsageResponse::from).toList();
        return new FamilyCustomersUsageResponse(
                familyCustomersUsage.familyId(),
                familyCustomersUsage.year(),
                familyCustomersUsage.month(),
                customers);
    }
}
