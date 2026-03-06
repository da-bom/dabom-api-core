package com.project.domain.mission.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.project.domain.mission.enums.MissionStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "mission_item",
        indexes = {
            @Index(name = "idx_mission_family", columnList = "family_id,status,created_at"),
            @Index(name = "idx_mission_creator", columnList = "created_by_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "target_customer_id", nullable = false)
    private Long targetId;

    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    @Column(name = "reward_template_id", nullable = false)
    private Long rewardTemplateId;

    @Column(name = "mission_text", columnDefinition = "TEXT", nullable = false)
    private String missionText;

    @Column(name = "reward_value", nullable = false)
    private Long rewardValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MissionStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public MissionItem(
            Long id,
            Long familyId,
            Long createdById,
            Long rewardTemplateId,
            String missionText,
            Long rewardValue,
            MissionStatus status,
            LocalDateTime completedAt) {
        this.id = id;
        this.familyId = familyId;
        this.createdById = createdById;
        this.rewardTemplateId = rewardTemplateId;
        this.missionText = missionText;
        this.rewardValue = rewardValue;
        this.status = status == null ? MissionStatus.ACTIVE : status;
        this.completedAt = completedAt;
    }

    public boolean canRequestReward() {
        return MissionStatus.ACTIVE.equals(this.status);
    }

    public void complete(LocalDateTime completedAt) {
        this.status = MissionStatus.COMPLETED;
        this.completedAt = completedAt == null ? LocalDateTime.now() : completedAt;
    }

    public void cancel() {
        this.status = MissionStatus.CANCELLED;
    }
}
