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

import com.project.domain.mission.enums.MissionRequestStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "mission_request",
        indexes = {
            @Index(name = "idx_mreq_mission", columnList = "mission_item_id,created_at"),
            @Index(name = "idx_mreq_requester", columnList = "requester_id,created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mission_item_id", nullable = false)
    private Long missionItemId;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MissionRequestStatus status;

    @Column(name = "resolved_by_id")
    private Long resolvedById;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public MissionRequest(
            Long id,
            Long missionItemId,
            Long requesterId,
            MissionRequestStatus status,
            Long resolvedById,
            LocalDateTime resolvedAt) {
        this.id = id;
        this.missionItemId = missionItemId;
        this.requesterId = requesterId;
        this.status = status == null ? MissionRequestStatus.PENDING : status;
        this.resolvedById = resolvedById;
        this.resolvedAt = resolvedAt;
    }

    public void approve(Long resolverId, LocalDateTime resolvedAt) {
        this.status = MissionRequestStatus.APPROVED;
        this.resolvedById = resolverId;
        this.resolvedAt = resolvedAt == null ? LocalDateTime.now() : resolvedAt;
    }

    public void reject(Long resolverId, LocalDateTime resolvedAt) {
        this.status = MissionRequestStatus.REJECTED;
        this.resolvedById = resolverId;
        this.resolvedAt = resolvedAt == null ? LocalDateTime.now() : resolvedAt;
    }
}
