package com.project.domain.mission.model;

import java.time.LocalDateTime;
import java.util.List;

import com.project.domain.reward.enums.RewardCategory;

/** 미션 목록 조회 서비스 결과 모델 */
public record MissionListResult(List<MissionCard> missions, String nextCursor, boolean hasNext) {

    /** 목록 카드 단위 모델 */
    public record MissionCard(
            Long missionItemId,
            String missionText,
            String requestStatus,
            CustomerSummary target,
            CustomerSummary createdBy,
            Reward reward,
            LocalDateTime createdAt) {}

    /** 사용자 요약 모델 */
    public record CustomerSummary(Long customerId, String name) {}

    /** 보상 요약 모델 */
    public record Reward(
            Long rewardId,
            String name,
            RewardCategory category,
            Long value,
            String unit,
            Long templateId) {}
}
