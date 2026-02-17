package com.project.domain.family.service;

import org.springframework.data.domain.Page;

import com.project.domain.family.model.FamilyUsageReport;
import com.project.domain.family.dto.request.FamilySearchRequest;
import com.project.domain.family.dto.response.FamilyDetailResponse;
import com.project.domain.family.dto.response.FamilySearchResponse;

public interface FamilyService {
    Page<FamilySearchResponse> searchFamilies(FamilySearchRequest familySearchRequest);

    FamilyDetailResponse getFamilyDetail(Long familyId);

    FamilyUsageReport getFamilyUsageReport(Long customerId, int year, int month);

    void handleFamilyEvent(Long familyId, Long customerId);
}
