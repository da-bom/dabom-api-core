package com.project.domain.reward.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.project.domain.reward.enums.RewardCategory;

class RewardTest {

    @Test
    @DisplayName("Reward는 템플릿과 스냅샷 값을 보관한다")
    void rewardKeepsSnapshotFields() {
        RewardTemplate template =
                RewardTemplate.builder()
                        .id(10L)
                        .name("메가커피 아메리카노")
                        .category(RewardCategory.GIFTICON)
                        .price(4500)
                        .isSystem(true)
                        .isActive(true)
                        .build();

        Reward reward =
                Reward.builder()
                        .id(20L)
                        .rewardTemplate(template)
                        .name("메가커피 아메리카노")
                        .category(RewardCategory.GIFTICON)
                        .thumbnailUrl("/rewards/mega-coffee.jpg")
                        .build();

        assertThat(reward.getId()).isEqualTo(20L);
        assertThat(reward.getRewardTemplate()).isSameAs(template);
        assertThat(reward.getName()).isEqualTo("메가커피 아메리카노");
        assertThat(reward.getCategory()).isEqualTo(RewardCategory.GIFTICON);
        assertThat(reward.getThumbnailUrl()).isEqualTo("/rewards/mega-coffee.jpg");
    }
}
