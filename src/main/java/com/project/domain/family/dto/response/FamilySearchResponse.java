package com.project.domain.family.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record FamilySearchResponse(
        Long familyId,
        String familyName,
        List<FamilyMemberSimpleResponse> customers,
        LocalDateTime createdAt) {}
