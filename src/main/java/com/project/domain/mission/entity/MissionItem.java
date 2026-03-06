package com.project.domain.mission.entity;

import java.time.LocalDateTime;
import java.util.Objects;

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
    private Long targetCustomerId;

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
            Long targetCustomerId,
            Long createdById,
            Long rewardTemplateId,
            String missionText,
            Long rewardValue,
            MissionStatus status,
            LocalDateTime completedAt) {
        this.id = id;
        this.familyId = familyId;
        this.targetCustomerId = targetCustomerId;
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

    /** ACTIVE 상태가 아니면 상태 전이 불가로 판단한다. */
    public void validateActive() {
        if (!MissionStatus.ACTIVE.equals(this.status)) {
            throw new IllegalStateException("Mission is not active");
        }
    }

    /** Mission 상태가 COMPLETED로 전이 가능한지 확인한다. */
    public boolean canComplete() {
        return MissionStatus.ACTIVE.equals(this.status);
    }

    /** Mission 상태가 CANCELLED로 전이 가능한지 확인한다. */
    public boolean canCancel() {
        return MissionStatus.ACTIVE.equals(this.status);
    }

    /** 요청자와 미션 대상자가 일치하는지 확인한다. */
    public boolean isAssignedTo(Long customerId) {
        return Objects.equals(this.targetCustomerId, customerId);
    }

    public void complete(LocalDateTime completedAt) {
        this.status = MissionStatus.COMPLETED;
        this.completedAt = completedAt == null ? LocalDateTime.now() : completedAt;
    }

    /** ACTIVE 미션을 COMPLETED로 완료 처리한다. */
    public void complete() {
        complete(LocalDateTime.now());
    }

    public void cancel() {
        this.status = MissionStatus.CANCELLED;
    }
}
