package com.project.domain.family.model;

import java.time.LocalDateTime;
import java.util.List;

public record FamilySearchResult(
        Long familyId,
        String familyName,
        List<FamilyMemberSummary> customers,
        LocalDateTime createdAt) {}
