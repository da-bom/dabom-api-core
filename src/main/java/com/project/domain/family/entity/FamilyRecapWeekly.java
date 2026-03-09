package com.project.domain.family.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.project.global.util.BaseEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "family_recap_weekly",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_family_recap_weekly_family_week",
                        columnNames = {"family_id", "week_start_date"})
        },
        indexes = {
                @Index(name = "idx_recap_weekly_family_week", columnList = "family_id, week_start_date")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FamilyRecapWeekly extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "total_used_bytes", nullable = false)
    private Long totalUsedBytes;

    @Column(name = "total_quota_bytes", nullable = false)
    private Long totalQuotaBytes;

    @Column(name = "usage_rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal usageRatePercent;

    @Column(name = "usage_by_weekday", columnDefinition = "json", nullable = false)
    private String usageByWeekday;

    @Column(name = "peak_usage", columnDefinition = "json")
    private String peakUsage;

    @Column(name = "mission_created_count", nullable = false)
    private int missionCreatedCount;

    @Column(name = "mission_completed_count", nullable = false)
    private int missionCompletedCount;

    @Column(name = "mission_rejected_count", nullable = false)
    private int missionRejectedCount;

    @Column(name = "appeal_count", nullable = false)
    private int appealCount;

    @Builder
    public FamilyRecapWeekly(
            Long id,
            Long familyId,
            LocalDate weekStartDate,
            Long totalUsedBytes,
            Long totalQuotaBytes,
            BigDecimal usageRatePercent,
            String usageByWeekday,
            String peakUsage,
            int missionCreatedCount,
            int missionCompletedCount,
            int missionRejectedCount,
            int appealCount) {
        this.id = id;
        this.familyId = familyId;
        this.weekStartDate = weekStartDate;
        this.totalUsedBytes = totalUsedBytes;
        this.totalQuotaBytes = totalQuotaBytes;
        this.usageRatePercent = usageRatePercent;
        this.usageByWeekday = usageByWeekday;
        this.peakUsage = peakUsage;
        this.missionCreatedCount = missionCreatedCount;
        this.missionCompletedCount = missionCompletedCount;
        this.missionRejectedCount = missionRejectedCount;
        this.appealCount = appealCount;
    }
}

