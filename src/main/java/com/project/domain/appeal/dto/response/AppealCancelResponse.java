package com.project.domain.appeal.dto.response;

import java.time.LocalDateTime;

import com.project.domain.appeal.enums.AppealStatus;
import com.project.domain.appeal.model.AppealCancelResult;

public record AppealCancelResponse(Long appealId, AppealStatus status, LocalDateTime cancelledAt) {
    public static AppealCancelResponse from(AppealCancelResult result) {
        return new AppealCancelResponse(
                result.appealId(), result.status(), result.cancelledAt());
    }
}
