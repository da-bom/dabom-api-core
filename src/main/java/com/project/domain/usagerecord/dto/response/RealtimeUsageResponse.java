package com.project.domain.usagerecord.dto.response;

public record RealtimeUsageResponse(
        Long familyId, Long totalUsedBytes, Long totalLimitBytes, Long remainingBytes) {}
