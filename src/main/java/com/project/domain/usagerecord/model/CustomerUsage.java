package com.project.domain.usagerecord.model;

public record CustomerUsage(
        Long customerId,
        String name,
        Long monthlyUsedBytes,
        Long monthlyLimitBytes,
        boolean isBlocked,
        String blockReason,
        boolean isMe) {}
