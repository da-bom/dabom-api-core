package com.project.domain.reward.model;

import java.time.LocalDateTime;
import java.util.List;

import com.project.domain.mission.model.MissionListResult;
import com.project.domain.mission.model.MissionLogListResult;

/** 보상 수령 내역 목록 모델 */
public record ReceivedRewardListResult(
        List<ReceivedRewardItem> rewards, String nextCursor, boolean hasNext) {

    /** 수령 내역 단건 모델 */
    public record ReceivedRewardItem(
            Long requestId,
            MissionLogListResult.MissionItemSimple missionItem,
            MissionListResult.CustomerSummary approvedBy,
            LocalDateTime approvedAt) {}
}
