package com.project.domain.usagerecord.dto.response;

import java.util.List;

// 홈화면 하단 dto
public record FamilyCustomersUsageResponse(
        Long familyId, int year, int month, List<CustomerUsage> customers) {}
