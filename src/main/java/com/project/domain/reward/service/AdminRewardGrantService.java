package com.project.domain.reward.service;

import org.springframework.data.domain.Page;

import com.project.domain.reward.entity.RewardGrant;
import com.project.domain.reward.enums.RewardGrantSort;
import com.project.domain.reward.enums.RewardGrantStatus;

public interface AdminRewardGrantService {

    Page<RewardGrant> getGrants(
            int page,
            int size,
            RewardGrantStatus status,
            RewardGrantSort sort,
            Boolean unusedOnly,
            String phoneNumber);
}
