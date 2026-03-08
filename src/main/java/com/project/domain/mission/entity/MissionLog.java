package com.project.domain.mission.entity;


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


import com.project.domain.mission.enums.MissionLogActionType;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Mission 상태 변경 이력을 저장하는 로그 엔티티다. */
@Entity
@Table(
        name = "mission_log",
        indexes = {
            @Index(name = "idx_mlog_mission", columnList = "mission_item_id,id"),
            @Index(name = "idx_mlog_actor", columnList = "actor_id,id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mission_item_id", nullable = false)
    private Long missionItemId;

    @Column(name = "actor_id")
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private MissionLogActionType actionType;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Builder
    public MissionLog(
            Long id,
            Long missionItemId,
            Long actorId,
            MissionLogActionType actionType,
            String message) {
        this.id = id;
        this.missionItemId = missionItemId;
        this.actorId = actorId;
        this.actionType = actionType;
        this.message = message;
    }
}
