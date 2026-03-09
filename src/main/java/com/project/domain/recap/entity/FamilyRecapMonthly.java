package com.project.domain.recap.entity;

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
        name = "family_recap_monthly",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_family_recap_monthly_family_month",
                    columnNames = {"family_id", "report_month"})
        },
        indexes = {
            @Index(name = "idx_recap_monthly_family_month", columnList = "family_id, report_month")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FamilyRecapMonthly extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "report_month", nullable = false)
    private LocalDate reportMonth;

    @Column(name = "total_used_bytes", nullable = false)
    private Long totalUsedBytes;

    @Column(name = "total_quota_bytes", nullable = false)
    private Long totalQuotaBytes;

    @Column(name = "usage_rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal usageRatePercent;

    @Column(name = "usage_by_weekday", columnDefinition = "json")
    private String usageByWeekday;

    @Column(name = "peak_usage", columnDefinition = "json")
    private String peakUsage;

    @Column(name = "mission_summary_json", columnDefinition = "json")
    private String missionSummaryJson;

    @Column(name = "appeal_summary_json", columnDefinition = "json")
    private String appealSummaryJson;

    @Column(name = "appeal_highlights_json", columnDefinition = "json")
    private String appealHighlightsJson;

    @Column(name = "communication_score", precision = 5, scale = 2)
    private BigDecimal communicationScore;

    @Builder
    public FamilyRecapMonthly(
            Long id,
            Long familyId,
            LocalDate reportMonth,
            Long totalUsedBytes,
            Long totalQuotaBytes,
            BigDecimal usageRatePercent,
            String usageByWeekday,
            String peakUsage,
            String missionSummaryJson,
            String appealSummaryJson,
            String appealHighlightsJson,
            BigDecimal communicationScore) {
        this.id = id;
        this.familyId = familyId;
        this.reportMonth = reportMonth;
        this.totalUsedBytes = totalUsedBytes;
        this.totalQuotaBytes = totalQuotaBytes;
        this.usageRatePercent = usageRatePercent;
        this.usageByWeekday = usageByWeekday;
        this.peakUsage = peakUsage;
        this.missionSummaryJson = missionSummaryJson;
        this.appealSummaryJson = appealSummaryJson;
        this.appealHighlightsJson = appealHighlightsJson;
        this.communicationScore = communicationScore;
    }
}
