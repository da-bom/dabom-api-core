package com.project.policy.web.dto.response;

import com.project.policy.core.PolicyType;
import com.project.policy.core.PolicyUpdateStatus;

public record PolicyUpdateResponse(ResultInfo result) {
    public static PolicyUpdateResponse success(Long userId, PolicyType type) {
        return new PolicyUpdateResponse(
                new ResultInfo(userId, type, PolicyUpdateStatus.APPLIED.name()));
    }

    public record ResultInfo(Long userId, PolicyType type, String status) {}
}
