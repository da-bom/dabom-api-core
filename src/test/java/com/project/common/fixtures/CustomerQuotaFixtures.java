package com.project.common.fixtures;

import java.time.LocalDate;

import com.project.domain.customer.entity.CustomerQuota;

public class CustomerQuotaFixtures {

    public static final Long DEFAULT_LIMIT = 5_000L;
    public static final LocalDate CURRENT_MONTH = LocalDate.now().withDayOfMonth(1);

    public static CustomerQuota quota(Long familyId, Long customerId, long usedBytes) {
        return CustomerQuota.builder()
                .familyId(familyId)
                .customerId(customerId)
                .monthlyLimitBytes(DEFAULT_LIMIT)
                .monthlyUsedBytes(usedBytes)
                .currentMonth(CURRENT_MONTH)
                .isBlocked(false)
                .blockReason(null)
                .build();
    }

    public static CustomerQuota blockedQuota(
            Long familyId, Long customerId, long usedBytes, String reason) {
        return CustomerQuota.builder()
                .familyId(familyId)
                .customerId(customerId)
                .monthlyLimitBytes(DEFAULT_LIMIT)
                .monthlyUsedBytes(usedBytes)
                .currentMonth(CURRENT_MONTH)
                .isBlocked(true)
                .blockReason(reason)
                .build();
    }
}
