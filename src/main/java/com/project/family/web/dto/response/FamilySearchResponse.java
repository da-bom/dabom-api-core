package com.project.family.web.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/** 가족 검색 결과 리스트의 개별 항목 DTO */
public record FamilySearchResponse(
        Long familyId,
        String familyName,
        List<FamilyMemberSimpleResponse> customers,
        LocalDateTime createdAt) {}
