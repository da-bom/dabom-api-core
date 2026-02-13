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

    public void update(String newRules, Boolean isActive, Long actorId) {
        if (newRules != null) {
            this.rules = newRules;
        }
        if (isActive != null) {
            boolean previousActiveState = this.isActive;
            this.isActive = isActive;

            if (isActive && !previousActiveState) {
                this.appliedAt = LocalDateTime.now();
                this.appliedById = actorId;
            }
        }
}
