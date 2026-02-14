package com.project.global.event.dto.policy;

public record PolicyUpdatedPayload(
        Long familyId,
        Long targetUserId,
        String policyKey,
        String oldValue,
        String newValue,
        Long changedBy) {}
