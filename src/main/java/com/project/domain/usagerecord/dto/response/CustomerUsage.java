package com.project.domain.usagerecord.dto.response;

// 사용자의 데이터 사용량 dto
public record CustomerUsage(
        Long customerId,
        String name,
        Long monthlyUsedBytes,
        Long monthlyLimitBytes,
        boolean isBlocked,
        String blockReason,
        boolean isMe) {}
