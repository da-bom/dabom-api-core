package com.project.domain.appeal.dto.response;

import java.time.LocalDateTime;

import com.project.domain.appeal.enums.AppealStatus;
import com.project.domain.appeal.model.AppealRespondResult;

public record AppealRespondResponse(
        Long appealId,
        AppealStatus status,
        String rejectReason,
        Long resolvedById,
        LocalDateTime resolvedAt) {
    public static AppealRespondResponse from(AppealRespondResult result) {
        return new AppealRespondResponse(
                result.appealId(),
                result.status(),
                result.rejectReason(),
                result.resolvedById(),
                result.resolvedAt());
    }
}
