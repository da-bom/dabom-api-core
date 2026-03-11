package com.project.domain.mission.enums;

/** 미션 이벤트 로그 타입.
 * CREATED: 부모가 미션을 생성함
 * REQUESTED: 자녀가 미션 완료를 요청함
 * CANCELLED: 부모가 미션을 취소함
 * COMPLETED: 부모가 요청을 승인해 미션이 완료됨
 */
public enum MissionLogActionType {
    CREATED,
    REQUESTED,
    CANCELLED,
    COMPLETED
}
