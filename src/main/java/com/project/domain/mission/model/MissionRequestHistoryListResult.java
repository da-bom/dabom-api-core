package com.project.domain.mission.model;

import java.time.LocalDateTime;
import java.util.List;

public record MissionRequestHistoryListResult(
        List<MissionRequestHistoryItem> requests, String nextCursor, boolean hasNext) {

    /** 미션 완료 요청 이력 한 건. requestId 단위로 한 번만 내려가며, status는 최신 처리 상태를 의미한다. */
    public record MissionRequestHistoryItem(
            Long requestId,
            String status,
            String rejectReason,
            MissionLogListResult.MissionItemSimple missionItem,
            MissionListResult.CustomerSummary requestedBy,
            MissionListResult.CustomerSummary respondedBy,
            LocalDateTime requestedAt,
            LocalDateTime respondedAt) {}
}
