package com.project.domain.customer.model;

import com.fasterxml.jackson.databind.JsonNode;

public record MyPageInfo(
        String name,
        String familyName,
        boolean isBlocked,
        String blockReason,
        Long monthlyLimitBytes,
        Long monthlyUsedBytes,
        JsonNode timeBlock) {}
