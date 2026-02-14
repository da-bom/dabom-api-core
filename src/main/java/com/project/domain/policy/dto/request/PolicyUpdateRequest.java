package com.project.domain.policy.dto.request;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.domain.policy.enums.PolicyType;

public record PolicyUpdateRequest(@Valid @NotNull UpdateInfo update) {
    public record UpdateInfo(
            @NotNull Long customerId,
            @NotNull PolicyType type,
            @JsonProperty("value") Map<String, Object> rules,
            Boolean isActive) {}
}
