package com.project.domain.appeal.model;

import java.time.LocalDateTime;

import com.project.domain.appeal.enums.AppealStatus;

public record AppealCancelResult(Long appealId, AppealStatus status, LocalDateTime cancelledAt) {}
