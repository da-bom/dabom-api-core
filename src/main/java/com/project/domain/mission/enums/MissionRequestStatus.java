package com.project.domain.mission.enums;

/** 미션 완료 요청 처리 상태. PENDING: 자녀가 요청했지만 아직 부모가 처리하지 않음 APPROVED: 부모가 요청을 승인함 REJECTED: 부모가 요청을 거절함 */
public enum MissionRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}
