package com.project.domain.mission.entity;

import java.time.LocalDateTime;

import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.MissionErrorCode;
import com.project.global.util.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;


import com.project.domain.mission.enums.MissionRequestStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "mission_request",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_mreq_pending_mission",
                    columnNames = "pending_mission_item_id")
        },
        indexes = {
            @Index(name = "idx_mreq_mission", columnList = "mission_item_id,created_at"),
            @Index(name = "idx_mreq_requester", columnList = "requester_id,created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionRequest extends BaseEntity {

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

    @Column(name = "reject_reason", columnDefinition = "TEXT", nullable = true)
    private String rejectReason;

    @Column(name = "resolved_by_id")
    private Long resolvedById;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "pending_mission_item_id", unique = true)
    private Long pendingMissionItemId;

    @Builder
    public MissionRequest(
            Long id,
            Long missionItemId,
            Long requesterId,
            MissionRequestStatus status,
            String rejectReason,
            Long resolvedById,
            LocalDateTime resolvedAt,
            Long pendingMissionItemId) {
        this.id = id;
        this.missionItemId = missionItemId;
        this.requesterId = requesterId;
        this.status = status == null ? MissionRequestStatus.PENDING : status;
        this.rejectReason = rejectReason;
        this.resolvedById = resolvedById;
        this.resolvedAt = resolvedAt;
        this.pendingMissionItemId =
                pendingMissionItemId != null
                        ? pendingMissionItemId
                        : MissionRequestStatus.PENDING.equals(this.status) ? missionItemId : null;
    }

    /** 현재 요청이 응답 가능한(PENDING) 상태인지 확인한다. */
    public boolean isPending() {
        return MissionRequestStatus.PENDING.equals(this.status);
    }

    /** PENDING 상태가 아니면 응답 처리할 수 없다. */
    public void validatePending() {
        if (!MissionRequestStatus.PENDING.equals(this.status)) {
            throw new ApplicationException(MissionErrorCode.MISSION_REQUEST_INVALID_STATUS);
        }
    }

    public void approve(Long resolverId, LocalDateTime resolvedAt) {
        this.status = MissionRequestStatus.APPROVED;
        this.rejectReason = null;
        this.resolvedById = resolverId;
        this.resolvedAt = resolvedAt == null ? LocalDateTime.now() : resolvedAt;
        this.pendingMissionItemId = null;
    }

    public void reject(Long resolverId, String rejectReason, LocalDateTime resolvedAt) {
        this.status = MissionRequestStatus.REJECTED;
        this.rejectReason = rejectReason;
        this.resolvedById = resolverId;
        this.resolvedAt = resolvedAt == null ? LocalDateTime.now() : resolvedAt;
        this.pendingMissionItemId = null;
    }
}
