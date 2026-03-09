package com.project.domain.appeal.entity;

import java.time.LocalDateTime;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.project.domain.appeal.enums.AppealStatus;
import com.project.domain.appeal.enums.AppealType;
import com.project.domain.customer.entity.Customer;
import com.project.domain.policy.entity.PolicyAssignment;
import com.project.global.util.BaseEntity;
import com.project.global.util.MapStringObjectConverter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 이의제기 엔티티 */
@Entity
@Table(name = "policy_appeal")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PolicyAppeal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AppealType type = AppealType.NORMAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_assignment_id")
    private PolicyAssignment policyAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private Customer requester;

    @Column(name = "request_reason", columnDefinition = "TEXT", nullable = false)
    private String requestReason;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "desired_rules", columnDefinition = "json")
    @Convert(converter = MapStringObjectConverter.class)
    private Map<String, Object> desiredRules;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AppealStatus status = AppealStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private Customer resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Override
    public String toString() {
        return "PolicyAppeal{"
                + "id="
                + id
                + ", type="
                + type
                + ", requestReason='"
                + requestReason
                + '\''
                + ", rejectReason='"
                + rejectReason
                + '\''
                + ", desiredRules="
                + desiredRules
                + ", status="
                + status
                + ", resolvedAt="
                + resolvedAt
                + ", cancelledAt="
                + cancelledAt
                + ", hasPolicyAssignment="
                + (policyAssignment != null)
                + ", hasRequester="
                + (requester != null)
                + ", hasResolvedBy="
                + (resolvedBy != null)
                + '}';
    }
}
