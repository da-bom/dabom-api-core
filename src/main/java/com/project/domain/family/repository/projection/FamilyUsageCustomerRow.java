package com.project.domain.family.repository.projection;

public record FamilyUsageCustomerRow(
        Long customerId,
        String name,
        Long monthlyUsedBytes,
        Long monthlyLimitBytes,
        boolean isBlocked,
        String blockReason) {}
