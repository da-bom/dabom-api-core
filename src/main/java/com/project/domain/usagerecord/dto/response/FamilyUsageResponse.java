package com.project.domain.usagerecord.dto.response;

// 홈화면 상단 dto
public record FamilyUsageResponse(
        Long familyId, String familyName, Long totalQuotaBytes, Long remainingBytes) {}
