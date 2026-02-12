package com.project.family.infra.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.project.customer.core.Role;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 가족 구성원 JPA 엔티티 */
@Entity
@Table(name = "family_member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FamilyMemberJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public FamilyMemberJpaEntity(Long id, Long familyId, Long customerId, Role role) {
        this.id = id;
        this.familyId = familyId;
        this.customerId = customerId;
        this.role = role;
    }
}
