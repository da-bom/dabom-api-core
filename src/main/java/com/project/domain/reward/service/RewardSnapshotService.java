package com.project.domain.reward.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.reward.entity.Reward;
import com.project.domain.reward.entity.RewardTemplate;
import com.project.domain.reward.repository.RewardRepository;
import com.project.domain.reward.repository.RewardTemplateRepository;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.MissionErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
/* 미션 생성 시점의 보상 템플릿 값을 복사해 Reward 스냅샷을 만든다. */
public class RewardSnapshotService {

    private final RewardRepository rewardRepository;
    private final RewardTemplateRepository rewardTemplateRepository;

    @Transactional
    /* 템플릿 현재값으로 미션 전용 Reward를 생성한다. */
    public Reward createFromTemplate(Long rewardTemplateId) {
        RewardTemplate rewardTemplate =
                rewardTemplateRepository
                        .findById(rewardTemplateId)
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                MissionErrorCode
                                                        .MISSION_REWARD_TEMPLATE_NOT_FOUND));

        return rewardRepository.save(
                Reward.builder()
                        .rewardTemplate(rewardTemplate)
                        .name(rewardTemplate.getName())
                        .category(rewardTemplate.getCategory())
                        .thumbnailUrl(rewardTemplate.getThumbnailUrl())
                        .build());
    }
}
