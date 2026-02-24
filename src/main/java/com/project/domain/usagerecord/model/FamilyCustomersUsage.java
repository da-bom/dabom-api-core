package com.project.domain.usagerecord.model;

import java.util.List;

public record FamilyCustomersUsage(Long familyId, int year, int month, List<CustomerUsage> customers) {}
