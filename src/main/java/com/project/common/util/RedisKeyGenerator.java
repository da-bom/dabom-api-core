package com.project.common.util;

import org.springframework.stereotype.Component;

@Component
public class RedisKeyGenerator {

    private static final String KEY_SEPARATOR = ":";
    private static final String FAMILY_KEY_PREFIX = "family";

    // 고객 정책 제약 키
    public String generateFamilyCustomerConstraintsKey(Long familyId, Long customerId) {
        return FAMILY_KEY_PREFIX
                + KEY_SEPARATOR
                + familyId
                + KEY_SEPARATOR
                + "customer"
                + KEY_SEPARATOR
                + customerId
                + KEY_SEPARATOR
                + "constraints";
    }
}
