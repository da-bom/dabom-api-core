package com.project.domain.reward.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;

import com.project.domain.customer.entity.Customer;
import com.project.domain.mission.entity.MissionItem;
import com.project.domain.reward.entity.Reward;
import com.project.domain.reward.entity.RewardGrant;
import com.project.domain.reward.enums.RewardCategory;
import com.project.domain.reward.enums.RewardGrantStatus;
import com.project.global.util.PhoneNumberUtils;

public record RewardGrantListResponse(
        List<RewardGrantItem> content, int page, int size, long totalElements, int totalPages) {

    public record RewardGrantItem(
            Long grantId,
            RewardInfo reward,
            CustomerInfo customer,
            MissionInfo mission,
            String couponCode,
            String couponUrl,
            RewardGrantStatus status,
            LocalDateTime expiredAt,
            LocalDateTime createdAt) {

        public static RewardGrantItem from(RewardGrant grant) {
            return new RewardGrantItem(
                    grant.getId(),
                    RewardInfo.from(grant.getReward()),
                    CustomerInfo.from(grant.getCustomer()),
                    MissionInfo.from(grant.getMissionItem()),
                    grant.getCouponCode(),
                    grant.getCouponUrl(),
                    grant.getStatus(),
                    grant.getExpiredAt(),
                    grant.getCreatedAt());
        }
    }

    public record RewardInfo(
            Long rewardId, String name, RewardCategory category, String thumbnailUrl) {

        public static RewardInfo from(Reward reward) {
            return new RewardInfo(
                    reward.getId(),
                    reward.getName(),
                    reward.getCategory(),
                    reward.getThumbnailUrl());
        }
    }

    public record CustomerInfo(Long customerId, String name, String phoneNumber) {

        public static CustomerInfo from(Customer customer) {
            return new CustomerInfo(
                    customer.getId(),
                    customer.getName(),
                    PhoneNumberUtils.mask(customer.getPhoneNumber()));
        }
    }

    public record MissionInfo(Long missionItemId, String missionText) {

        public static MissionInfo from(MissionItem missionItem) {
            return new MissionInfo(missionItem.getId(), missionItem.getMissionText());
        }
    }

    public static RewardGrantListResponse from(Page<RewardGrant> grants) {
        List<RewardGrantItem> items =
                grants.getContent().stream().map(RewardGrantItem::from).toList();
        return new RewardGrantListResponse(
                items,
                grants.getNumber(),
                grants.getSize(),
                grants.getTotalElements(),
                grants.getTotalPages());
    }
}
