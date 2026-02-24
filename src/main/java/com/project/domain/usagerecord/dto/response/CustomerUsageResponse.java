package com.project.domain.usagerecord.dto.response;

// 사용자의 데이터 사용량 dto
public record CustomerUsageResponse(
        Long customerId,
        String name,
        Long monthlyUsedBytes,
        Long monthlyLimitBytes,
        boolean isBlocked,
        String blockReason,
        boolean isMe) {
    public static CustomerUsageResponse from(
            com.project.domain.usagerecord.model.CustomerUsage customerUsage) {
        return new CustomerUsageResponse(
                customerUsage.customerId(),
                customerUsage.name(),
                customerUsage.monthlyUsedBytes(),
                customerUsage.monthlyLimitBytes(),
                customerUsage.isBlocked(),
                customerUsage.blockReason(),
                customerUsage.isMe());
    }
}
