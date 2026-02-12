package com.project.customer.infra.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.springframework.data.annotation.LastModifiedDate;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customer_quota")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerQuotaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "monthly_limit_bytes")
    private Long monthlyLimitBytes;

    @Column(name = "monthly_used_bytes", nullable = false)
    private Long monthlyUsedBytes;

    @Column(name = "current_month", nullable = false)
    private LocalDate currentMonth;

    @Column(name = "is_blocked", nullable = false)
    private boolean isBlocked;

    @Column(name = "block_reason")
    private String blockReason;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public CustomerQuotaJpaEntity(
            Long customerId,
            Long familyId,
            Long monthlyLimitBytes,
            Long monthlyUsedBytes,
            LocalDate currentMonth,
            boolean isBlocked,
            String blockReason) {
        this.customerId = customerId;
        this.familyId = familyId;
        this.monthlyLimitBytes = monthlyLimitBytes;
        this.monthlyUsedBytes = monthlyUsedBytes;
        this.currentMonth = currentMonth;
        this.isBlocked = isBlocked;
        this.blockReason = blockReason;
    }
}
