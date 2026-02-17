package com.project.domain.family.dto.response;

import java.util.List;

import com.project.domain.family.model.FamilyUsageReport;

public record FamilyUsageReportResponse(
        Long familyId,
        String familyName,
        int year,
        int month,
        Long totalQuotaBytes,
        Long remainingBytes,
        List<CustomerUsage> customers) {

    public static FamilyUsageReportResponse from(FamilyUsageReport report) {
        List<CustomerUsage> customers =
                report.customers().stream()
                        .map(
                                customer ->
                                        new CustomerUsage(
                                                customer.customerId(),
                                                customer.name(),
                                                customer.monthlyUsedBytes(),
                                                customer.monthlyLimitBytes(),
                                                customer.isBlocked(),
                                                customer.blockReason(),
                                                customer.isMe()))
                        .toList();

        return new FamilyUsageReportResponse(
                report.familyId(),
                report.familyName(),
                report.year(),
                report.month(),
                report.totalQuotaBytes(),
                report.remainingBytes(),
                customers);
    }

    public record CustomerUsage(
            Long customerId,
            String name,
            Long monthlyUsedBytes,
            Long monthlyLimitBytes,
            boolean isBlocked,
            String blockReason,
            boolean isMe) {}
}
