package com.project.domain.customer.repository;

import java.util.List;

import com.project.domain.usagerecord.dto.response.CustomerUsageResponse;

public interface CustomerQuotaQueryRepository {
    List<CustomerUsageResponse> findCustomerUsage(Long familyId, Long customerId);
}
