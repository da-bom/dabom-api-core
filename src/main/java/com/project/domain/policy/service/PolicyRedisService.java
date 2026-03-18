package com.project.domain.policy.service;

import java.util.Map;

import com.project.domain.policy.enums.PolicyType;

public interface PolicyRedisService {
    void syncToRedis(
            Long familyId,
            Long targetCustomerId,
            PolicyType type,
            Map<String, Object> rules,
            boolean isActive);
}
