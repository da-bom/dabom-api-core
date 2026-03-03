package com.project.domain.usagerecord.model;

// usagerecord 도메인에서 서비스와 API 계층 사이에 전달되는 데이터 객체입니다.
public record CustomerUsage(
        Long customerId,
        String name,
        Long monthlyUsedBytes,
        Long monthlyLimitBytes,
        boolean isBlocked,
        String blockReason,
        boolean isMe) {}
