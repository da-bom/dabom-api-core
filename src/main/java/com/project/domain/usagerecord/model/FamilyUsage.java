package com.project.domain.usagerecord.model;

// 가족 데이터 사용량 집계용 모델
public record FamilyUsage(
        Long familyId, String familyName, Long totalQuotaBytes, Long remainingBytes) {}
