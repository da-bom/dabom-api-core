package com.project.policy.core;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PolicyAssignment {
    private Long id;
    private Long policyId;
    private Long familyId;
    private Long targetCustomerId;
    private String rules;
    private boolean isActive;
    private LocalDateTime appliedAt;
    private Long appliedById;
    private LocalDateTime deletedAt;

    public void updateRules(String newRules) {
        this.rules = newRules;
    }

    public void toggleActive(boolean active, Long actorId) {
        if (active && !this.isActive) {
            this.appliedAt = LocalDateTime.now();
            this.appliedById = actorId;
        }
        this.isActive = active;
    }
}
