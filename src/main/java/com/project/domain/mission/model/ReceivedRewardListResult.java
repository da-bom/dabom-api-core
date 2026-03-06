package com.project.domain.mission.model;

import java.time.LocalDateTime;
import java.util.List;

/** 보상 수령 내역 목록 모델이다. */
public record ReceivedRewardListResult(
        List<ReceivedRewardItem> content, String nextCursor, boolean hasNext) {

    /** 수령 내역 단건 모델이다. */
    public record ReceivedRewardItem(
            Long requestId,
            MissionLogListResult.MissionItemSimple missionItem,
            MissionListResult.CustomerSummary approvedBy,
            LocalDateTime approvedAt) {}
}
