package com.project.policy.web.dto.response;

import com.project.policy.core.PolicyType;

public record PolicyUpdateResponse(ResultInfo result) {
    public static PolicyUpdateResponse success(Long userId, PolicyType type) {
        return new PolicyUpdateResponse(new ResultInfo(userId, type, "APPLIED"));
    }

    public record ResultInfo(Long userId, PolicyType type, String status) {}
}
