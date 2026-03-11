package com.project.domain.reward.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.reward.dto.response.RewardGrantListResponse;
import com.project.domain.reward.entity.RewardGrant;
import com.project.domain.reward.enums.RewardGrantStatus;
import com.project.domain.reward.repository.RewardGrantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminRewardGrantService {

    private final RewardGrantRepository rewardGrantRepository;

    public RewardGrantListResponse getGrants(
            int page,
            int size,
            RewardGrantStatus status,
            String sort,
            Boolean unusedOnly,
            String phoneNumber) {

        if (Boolean.TRUE.equals(unusedOnly)) {
            status = RewardGrantStatus.ISSUED;
        }

        Sort sortOrder =
                "EXPIRING_SOON".equals(sort)
                        ? Sort.by("expiredAt").ascending()
                        : Sort.by("createdAt").descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<RewardGrant> grants =
                rewardGrantRepository.findWithFilters(status, phoneNumber, pageable);

        return RewardGrantListResponse.from(grants);
    }
}
