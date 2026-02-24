package com.project.domain.usagerecord.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RealtimeFamilyUsageResponse(
        Long familyId, Integer year, Integer month, List<CustomerUsageResponse> customers) {}
