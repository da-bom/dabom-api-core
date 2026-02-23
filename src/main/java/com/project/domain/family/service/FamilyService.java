package com.project.domain.family.service;

import org.springframework.data.domain.Page;

import com.project.domain.family.dto.request.FamilySearchRequest;
import com.project.domain.family.model.FamilyDetail;
import com.project.domain.family.model.FamilySearchResult;

public interface FamilyService {
    Page<FamilySearchResult> searchFamilies(FamilySearchRequest familySearchRequest);

    FamilyDetail getFamilyDetail(Long familyId);

    void handleFamilyEvent(Long familyId, Long customerId);
}
