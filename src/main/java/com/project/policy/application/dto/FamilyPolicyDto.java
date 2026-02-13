package com.project.policy.application.dto;

import com.project.policy.core.PolicyType;
import com.querydsl.core.annotations.QueryProjection;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FamilyPolicyDto {
    private Long familyId;
    private Long customerId;
    private String customerName;
    private String phoneNumber;
    private String role; // FamilyMemberRole

    // PolicyAssignment
    private Long assignmentId;
    private Long policyId;
    private String policyName;
    private PolicyType type;
    private boolean isActive;
    private String rules;

    @QueryProjection
    public FamilyPolicyDto(
            Long familyId,
            Long customerId,
            String customerName,
            String phoneNumber,
            String role,
            Long assignmentId,
            Long policyId,
            String policyName,
            PolicyType type,
            boolean isActive,
            String rules) {
        this.familyId = familyId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.assignmentId = assignmentId;
        this.policyId = policyId;
        this.policyName = policyName;
        this.type = type;
        this.isActive = isActive;
        this.rules = rules;
    }
}
