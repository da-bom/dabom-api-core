package com.project.domain.reward.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.project.domain.reward.enums.RewardCategory;
import com.project.common.util.BaseEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reward")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reward extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reward_template_id", nullable = false)
    private RewardTemplate rewardTemplate;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private RewardCategory category;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Builder
    public Reward(
            Long id,
            RewardTemplate rewardTemplate,
            String name,
            RewardCategory category,
            String thumbnailUrl) {
        this.id = id;
        this.rewardTemplate = rewardTemplate;
        this.name = name;
        this.category = category;
        this.thumbnailUrl = thumbnailUrl;
    }

    @Override
    public String toString() {
        return "Reward{"
                + "id="
                + id
                + ", name='"
                + name
                + '\''
                + ", category="
                + category
                + ", thumbnailUrl='"
                + thumbnailUrl
                + '\''
                + '}';
    }
}
