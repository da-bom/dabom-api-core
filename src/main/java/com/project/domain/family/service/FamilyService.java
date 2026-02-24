package com.project.domain.family.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;

import com.project.domain.family.dto.request.FamilySearchRequest;
import com.project.domain.family.entity.Family;
import com.project.domain.family.model.FamilyDetail;
import com.project.domain.family.model.FamilySearchResult;
import com.project.domain.family.repository.projection.FamilyUsageCustomerRow;

public interface FamilyService {
    Page<FamilySearchResult> searchFamilies(FamilySearchRequest familySearchRequest);

    FamilyDetail getFamilyDetail(Long familyId);

    Long getFamilyIdByCustomerId(Long customerId);

    Family getFamilyById(Long familyId);

    List<FamilyUsageCustomerRow> getUsageReportCustomers(Long familyId, LocalDate targetMonth);

    void handleFamilyEvent(Long familyId, Long customerId);
}
