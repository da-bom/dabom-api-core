package com.project.domain.policy.dto.response;

import com.project.domain.policy.enums.PolicyType;
import com.project.domain.policy.enums.PolicyUpdateStatus;

public record PolicyUpdateResponse(ResultInfo result) {
    public static PolicyUpdateResponse success(Long userId, PolicyType type) {
        return new PolicyUpdateResponse(
                new ResultInfo(userId, type, PolicyUpdateStatus.APPLIED.name()));
    }

    public record ResultInfo(Long userId, PolicyType type, String status) {}
}
