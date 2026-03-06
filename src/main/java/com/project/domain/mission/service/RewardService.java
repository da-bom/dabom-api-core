package com.project.domain.mission.service;

import com.project.domain.mission.dto.request.RespondRewardRequest;
import com.project.domain.mission.model.AuthContext;
import com.project.domain.mission.model.ReceivedRewardListResult;
import com.project.domain.mission.model.RewardRespondResult;

/** 미션 보상 요청 처리 로직 인터페이스다. */
public interface RewardService {
    RewardRespondResult respondRewardRequest(AuthContext auth, Long requestId, RespondRewardRequest req);

    ReceivedRewardListResult listReceivedRewards(AuthContext auth, String cursor, int size);
}
