package com.project.domain.reward.service;

import com.project.domain.reward.enums.RewardGrantSort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.reward.entity.RewardGrant;
import com.project.domain.reward.enums.RewardGrantStatus;
import com.project.domain.reward.repository.RewardGrantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminRewardGrantServiceImpl implements AdminRewardGrantService {

    private final RewardGrantRepository rewardGrantRepository;

    @Override
    public Page<RewardGrant> getGrants(
            int page,
            int size,
            RewardGrantStatus status,
            RewardGrantSort sort,
            Boolean unusedOnly,
            String phoneNumber) {

        if (Boolean.TRUE.equals(unusedOnly)) {
            status = RewardGrantStatus.ISSUED;
        }

        Sort sortOrder =
                sort == RewardGrantSort.EXPIRING_SOON
                        ? Sort.by("expiredAt").ascending()
                        : Sort.by("createdAt").descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        return rewardGrantRepository.findWithFilters(status, phoneNumber, pageable);
    }
}
