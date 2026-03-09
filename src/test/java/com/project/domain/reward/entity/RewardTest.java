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
                        .name("용돈")
                        .category(RewardCategory.ETC)
                        .defaultValue(1000L)
                        .unit("원")
                        .isSystem(true)
                        .build();

        Reward reward =
                Reward.builder()
                        .id(20L)
                        .rewardTemplate(template)
                        .name("용돈")
                        .category(RewardCategory.ETC)
                        .value(5000L)
                        .unit("원")
                        .build();

        assertThat(reward.getId()).isEqualTo(20L);
        assertThat(reward.getRewardTemplate()).isSameAs(template);
        assertThat(reward.getName()).isEqualTo("용돈");
        assertThat(reward.getCategory()).isEqualTo(RewardCategory.ETC);
        assertThat(reward.getValue()).isEqualTo(5000L);
        assertThat(reward.getUnit()).isEqualTo("원");
    }
}
