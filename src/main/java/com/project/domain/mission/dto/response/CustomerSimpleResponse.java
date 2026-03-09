package com.project.domain.mission.dto.response;

import com.project.domain.mission.model.MissionListResult;

/** 사용자 요약 응답 DTO다. */
public record CustomerSimpleResponse(Long customerId, String name) {
    public static CustomerSimpleResponse from(MissionListResult.CustomerSummary summary) {
        if (summary == null) {
            return null;
        }
        return new CustomerSimpleResponse(summary.customerId(), summary.name());
    }
}
