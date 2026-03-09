package com.project.domain.mission.model;

import java.time.LocalDateTime;
import java.util.List;

import com.project.domain.mission.enums.RewardCategory;

/** 미션 목록 조회 서비스 결과 모델이다. */
public record MissionListResult(List<MissionCard> missions, String nextCursor, boolean hasNext) {

    /** 목록 카드 단위 모델이다. */
    public record MissionCard(
            Long missionItemId,
            String missionText,
            String requestStatus,
            CustomerSummary target,
            CustomerSummary createdBy,
            RewardTemplate rewardTemplate,
            Long rewardValue,
            LocalDateTime createdAt) {}

    /** 사용자 요약 모델이다. */
    public record CustomerSummary(Long customerId, String name) {}

    /** 보상 템플릿 요약 모델이다. */
    public record RewardTemplate(Long id, String name, RewardCategory category, String unit) {}
}
