package com.project.policy.infra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.project.customer.infra.entity.BaseJpaEntity;
import com.project.policy.core.PolicyType;
import com.project.policy.core.RequireRole;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "policy")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PolicyJpaEntity extends BaseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "require_role", nullable = false)
    private RequireRole requireRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyType type;

    @Column(name = "default_rules", columnDefinition = "json", nullable = false)
    private String defaultRules;

    @Column(name = "is_system", columnDefinition = "tinyint", nullable = false)
    private boolean isSystem;
}
