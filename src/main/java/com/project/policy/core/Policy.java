package com.project.policy.core;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Policy {
    private Long id;
    private String name;
    private String description;
    private RequireRole requireRole;
    private PolicyType type;
    private String defaultRules;
    private boolean isSystem;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public boolean isActive() {
        return deletedAt == null;
    }

    public boolean isModifiable() {
        return !isSystem && isActive();
    }
}
