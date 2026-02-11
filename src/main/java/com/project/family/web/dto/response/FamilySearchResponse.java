package com.project.family.web.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record FamilySearchResponse(
        Long familyId,
        String familyName,
        int customerCount,
        List<FamilyMemberSimpleResponse> customers,
        LocalDateTime createdAt) {}
