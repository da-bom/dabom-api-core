package com.project.domain.reward.support;

import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.MissionErrorCode;
import com.project.domain.mission.model.MissionListResult;
import com.project.domain.reward.entity.Reward;

/** Reward 엔티티를 공통 응답 모델로 변환한다. */
public final class RewardDtoMapper {

    private RewardDtoMapper() {}

    public static MissionListResult.Reward toModel(Reward reward) {
        if (reward == null || reward.getRewardTemplate() == null) {
            throw new ApplicationException(MissionErrorCode.MISSION_REWARD_TEMPLATE_NOT_FOUND);
        }
        return new MissionListResult.Reward(
                reward.getId(),
                reward.getName(),
                reward.getCategory(),
                reward.getThumbnailUrl(),
                reward.getRewardTemplate().getId());
    }
}
