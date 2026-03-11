package com.project.domain.reward.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.project.domain.customer.entity.Customer;
import com.project.domain.mission.entity.MissionItem;
import com.project.domain.reward.enums.RewardGrantStatus;
import com.project.global.util.BaseEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "reward_grant",
        indexes = {
            @Index(name = "idx_reward_grant_status_created", columnList = "status, created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardGrant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reward_id", nullable = false)
    private Reward reward;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_item_id", nullable = false)
    private MissionItem missionItem;

    @Column(name = "coupon_code", length = 100)
    private String couponCode;

    @Column(name = "coupon_url", length = 255)
    private String couponUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RewardGrantStatus status;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Builder
    public RewardGrant(
            Long id,
            Reward reward,
            Customer customer,
            MissionItem missionItem,
            String couponCode,
            String couponUrl,
            RewardGrantStatus status,
            LocalDateTime expiredAt) {
        this.id = id;
        this.reward = reward;
        this.customer = customer;
        this.missionItem = missionItem;
        this.couponCode = couponCode;
        this.couponUrl = couponUrl;
        this.status = status;
        this.expiredAt = expiredAt;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RewardGrant.RewardGrantBuilder(");
        sb.append("id=").append(this.id);
        sb.append(", reward=").append(this.reward);
        sb.append(", customer=").append(this.customer);
        // Avoid calling MissionItem.toString() directly.
        if (this.missionItem != null) {
            sb.append(", missionItem=<set>");
        } else {
            sb.append(", missionItem=null");
        }
        sb.append(", couponCode=").append(this.couponCode);
        sb.append(", couponUrl=").append(this.couponUrl);
        sb.append(", status=").append(this.status);
        sb.append(", expiredAt=").append(this.expiredAt);
        sb.append(')');
        return sb.toString();
    }
}
