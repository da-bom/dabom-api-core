package com.project.domain.usagerecord.dto.response;

public record RealtimeTotalUsageResponse(
        Long familyId, Long totalUsedBytes, Long totalLimitBytes, Long remainingBytes) {}
