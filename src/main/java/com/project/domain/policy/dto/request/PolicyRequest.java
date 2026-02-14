package com.project.domain.policy.dto.request;

import java.util.Map;

import com.project.domain.customer.enums.RoleType;
import com.project.domain.policy.enums.PolicyType;

import io.swagger.v3.oas.annotations.media.Schema;

public class PolicyRequest {

    public record Create(String name, PolicyType policyType) {}

    public record Update(
            String description,
            RoleType requiredRole,
            PolicyType policyType,
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
                    Map<String, Object> defaultRules,
            boolean isActive) {}
}
