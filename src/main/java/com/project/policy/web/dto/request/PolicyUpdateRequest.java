package com.project.policy.web.dto.request;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.policy.core.PolicyType;

public record PolicyUpdateRequest(@Valid @NotNull UpdateInfo update) {
    public record UpdateInfo(
            @NotNull Long customerId,
            @NotNull PolicyType type,

            // Nullable (규칙 수정 시에만 보냄)
            @JsonProperty("value") Map<String, Object> rules,

            // Nullable (활성 상태 변경 시에만 보냄)
            Boolean isActive) {}
}
