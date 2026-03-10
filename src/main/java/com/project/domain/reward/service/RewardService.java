package com.project.domain.reward.service;

import com.project.domain.reward.dto.request.RespondRewardRequest;
import com.project.domain.reward.model.ReceivedRewardListResult;
import com.project.domain.reward.model.RewardRespondResult;
import com.project.global.auth.model.AuthContext;

/** 미션 보상 요청 처리 로직 인터페이스다. */
public interface RewardService {
    RewardRespondResult respondRewardRequest(
            AuthContext auth, Long requestId, RespondRewardRequest req);

    ReceivedRewardListResult listReceivedRewards(AuthContext auth, String cursor, int size);
}
