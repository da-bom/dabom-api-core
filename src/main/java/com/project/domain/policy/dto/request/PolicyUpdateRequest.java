package com.project.domain.policy.dto.request;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.domain.policy.enums.PolicyType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "구성원 정책 수정 요청")
public record PolicyUpdateRequest(
        @Schema(description = "정책 수정 정보", requiredMode = Schema.RequiredMode.REQUIRED)
                @Valid
                @NotNull
                UpdateInfo update) {
    @Schema(description = "정책 수정 대상 정보")
    public record UpdateInfo(
            @Schema(
                            description = "수정 대상 고객 ID",
                            example = "12347",
                            requiredMode = Schema.RequiredMode.REQUIRED)
                    @NotNull
                    Long customerId,
            @Schema(
                            description = "정책 타입",
                            example = "MONTHLY_LIMIT",
                            requiredMode = Schema.RequiredMode.REQUIRED)
                    @NotNull
                    PolicyType type,
            @Schema(
                            description = "정책 rule 값(JSON 형태). 정책 타입별로 구조가 달라질 수 있음",
                            example =
                                    """
                            { "limitBytes": 3221225472 }
                            """)
                    @JsonProperty("value")
                    Map<String, Object> rules,
            @Schema(description = "정책 활성화 여부(미전달 시 기존 값 유지)", example = "true", nullable = true)
                    Boolean isActive) {}
}
