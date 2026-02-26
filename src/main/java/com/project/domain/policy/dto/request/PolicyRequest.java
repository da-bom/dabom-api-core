package com.project.domain.policy.dto.request;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

import com.project.domain.customer.enums.RoleType;
import com.project.domain.policy.enums.PolicyType;

import io.swagger.v3.oas.annotations.media.Schema;

public class PolicyRequest {

    public record Create(@NotNull String name, @NotNull PolicyType type) {}

    public record Update(
            @NotNull String description,
            @NotNull RoleType requireRole,
            @NotNull PolicyType type,
            @Schema(
                            description = "정책별 세부 규칙 (JSON)",
                            example =
                                    """
                                    {
                                      "start": "22:00",
                                      "end": "07:00",
                                      "timezone": "Asia/Seoul"
                                    }
                                    """)
                    @NotNull Map<String, Object> defaultRules,
            @NotNull Boolean isActive,
            @NotNull Boolean overWrite) {}
}
