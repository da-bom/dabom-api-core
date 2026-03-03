package com.project.domain.usagerecord.dto.response;

import java.util.List;

import com.project.domain.usagerecord.model.FamilyCustomersUsage;

// FamilyController.getCustomersUsageDashboard에서 반환할 때 FamilyCustomersUsage 모델을 API 응답으로 변환하는 DTO
public record FamilyCustomersUsageResponse(
        Long familyId,
        String familyName,
        int year,
        int month,
        Long totalQuotaBytes,
        Long remainingBytes,
        double usedPercent,
        List<CustomerUsageResponse> customers) {
    public static FamilyCustomersUsageResponse from(FamilyCustomersUsage familyCustomersUsage) {
        List<CustomerUsageResponse> customers =
                familyCustomersUsage.customers().stream().map(CustomerUsageResponse::from).toList();
        return new FamilyCustomersUsageResponse(
                familyCustomersUsage.familyId(),
                familyCustomersUsage.familyName(),
                familyCustomersUsage.year(),
                familyCustomersUsage.month(),
                familyCustomersUsage.totalQuotaBytes(),
                familyCustomersUsage.remainingBytes(),
                familyCustomersUsage.usedPercent(),
                customers);
    }
}
