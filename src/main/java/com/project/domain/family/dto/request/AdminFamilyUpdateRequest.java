package com.project.domain.family.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.project.common.auth.enums.RoleType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "가족 구성원 권한/한도 일괄 수정 요청")
public record AdminFamilyUpdateRequest(
        @Schema(description = "수정할 구성원 목록") @Valid @NotNull List<MemberUpdate> members) {

    @Schema(description = "구성원 수정 정보")
    public record MemberUpdate(
            @Schema(description = "구성원 ID", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull
                    Long customerId,
            @Schema(
                            description = "변경할 역할",
                            example = "OWNER",
                            requiredMode = Schema.RequiredMode.REQUIRED)
                    @NotNull
                    RoleType role,
            @Schema(
                            description = "월간 데이터 한도 (bytes)",
                            example = "32212254720",
                            requiredMode = Schema.RequiredMode.REQUIRED)
                    @NotNull
                    Long monthlyLimitBytes) {}
}
