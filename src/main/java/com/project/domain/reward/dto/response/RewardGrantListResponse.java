package com.project.domain.reward.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.project.domain.reward.entity.RewardGrant;
import com.project.domain.reward.enums.RewardCategory;
import com.project.domain.reward.enums.RewardGrantStatus;

import org.springframework.data.domain.Page;

public record RewardGrantListResponse(
        List<RewardGrantItem> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public record RewardGrantItem(
            Long grantId,
            RewardInfo reward,
            CustomerInfo customer,
            MissionInfo mission,
            String couponCode,
            String couponUrl,
            RewardGrantStatus status,
            LocalDateTime expiredAt,
            LocalDateTime createdAt) {}

    public record RewardInfo(
            Long rewardId, String name, RewardCategory category, String thumbnailUrl) {}

    public record CustomerInfo(Long customerId, String name, String phoneNumber) {}

    public record MissionInfo(Long missionItemId, String missionText) {}

    public static RewardGrantListResponse from(Page<RewardGrant> grants) {
        List<RewardGrantItem> items =
                grants.getContent().stream()
                        .map(
                                grant ->
                                        new RewardGrantItem(
                                                grant.getId(),
                                                new RewardInfo(
                                                        grant.getReward().getId(),
                                                        grant.getReward().getName(),
                                                        grant.getReward().getCategory(),
                                                        grant.getReward().getThumbnailUrl()),
                                                new CustomerInfo(
                                                        grant.getCustomer().getId(),
                                                        grant.getCustomer().getName(),
                                                        grant.getCustomer().getPhoneNumber()),
                                                new MissionInfo(
                                                        grant.getMissionItem().getId(),
                                                        grant.getMissionItem().getMissionText()),
                                                grant.getCouponCode(),
                                                grant.getCouponUrl(),
                                                grant.getStatus(),
                                                grant.getExpiredAt(),
                                                grant.getCreatedAt()))
                        .toList();
        return new RewardGrantListResponse(
                items,
                grants.getNumber(),
                grants.getSize(),
                grants.getTotalElements(),
                grants.getTotalPages());
    }
}
