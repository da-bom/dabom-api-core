package com.project.policy.infra.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.project.customer.infra.entity.BaseJpaEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "policy_assignment")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyAssignmentJpaEntity extends BaseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long policyId;

    @Column(nullable = false)
    private Long familyId;

    @Column(name = "target_customer_id")
    private Long targetCustomerId;

    // JSON 타입은 문자열로 저장
    @Column(columnDefinition = "json", nullable = false)
    private String rules;

    @Column(nullable = false)
    private boolean isActive;

    private LocalDateTime appliedAt;

    private Long appliedById;
}
