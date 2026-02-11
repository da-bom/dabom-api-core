package com.project.family.infra.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.project.global.BaseEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 가족 JPA 엔티티
 * - DB 매핑 전용: 도메인 로직 없음
 */
@Entity
@Table(name = "family")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE family SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class FamilyJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    @Column(name = "total_quota_bytes", nullable = false)
    private Long totalQuotaBytes;

    @Column(name = "used_bytes", nullable = false)
    private Long usedBytes;

    @Column(name = "current_month", nullable = false)
    private LocalDate currentMonth;

    @Builder
    public FamilyJpaEntity(
            Long id,
            String name,
            Long createdById,
            Long totalQuotaBytes,
            Long usedBytes,
            LocalDate currentMonth) {
        this.id = id;
        this.name = name;
        this.createdById = createdById;
        this.totalQuotaBytes = totalQuotaBytes;
        this.usedBytes = usedBytes;
        this.currentMonth = currentMonth;
    }
}
