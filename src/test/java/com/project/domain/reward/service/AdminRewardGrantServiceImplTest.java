package com.project.domain.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.project.domain.reward.entity.RewardGrant;
import com.project.domain.reward.enums.RewardGrantSort;
import com.project.domain.reward.enums.RewardGrantStatus;
import com.project.domain.reward.repository.RewardGrantRepository;

@ExtendWith(MockitoExtension.class)
class AdminRewardGrantServiceImplTest {

    @Mock private RewardGrantRepository rewardGrantRepository;

    @InjectMocks private AdminRewardGrantServiceImpl adminRewardGrantService;

    @Test
    @DisplayName("기본 조회 - 필터 없이 LATEST 정렬로 조회한다")
    void getGrants_noFilters_returnsLatestSorted() {
        // given
        Page<RewardGrant> page = new PageImpl<>(List.of());
        given(rewardGrantRepository.findWithFilters(eq(null), eq(null), any(Pageable.class)))
                .willReturn(page);

        // when
        Page<RewardGrant> result =
                adminRewardGrantService.getGrants(0, 20, null, RewardGrantSort.LATEST, null, null);

        // then
        assertThat(result).isEqualTo(page);
        verify(rewardGrantRepository).findWithFilters(eq(null), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("status 필터 - 전달된 status로 필터링한다")
    void getGrants_withStatus_filtersByStatus() {
        // given
        Page<RewardGrant> page = new PageImpl<>(List.of());
        given(
                        rewardGrantRepository.findWithFilters(
                                eq(RewardGrantStatus.USED), eq(null), any(Pageable.class)))
                .willReturn(page);

        // when
        Page<RewardGrant> result =
                adminRewardGrantService.getGrants(
                        0, 20, RewardGrantStatus.USED, RewardGrantSort.LATEST, null, null);

        // then
        assertThat(result).isEqualTo(page);
        verify(rewardGrantRepository)
                .findWithFilters(eq(RewardGrantStatus.USED), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("unusedOnly=true - status를 ISSUED로 오버라이드한다")
    void getGrants_unusedOnly_overridesStatusToIssued() {
        // given
        Page<RewardGrant> page = new PageImpl<>(List.of());
        given(
                        rewardGrantRepository.findWithFilters(
                                eq(RewardGrantStatus.ISSUED), eq(null), any(Pageable.class)))
                .willReturn(page);

        // when
        adminRewardGrantService.getGrants(
                0, 20, RewardGrantStatus.USED, RewardGrantSort.LATEST, true, null);

        // then
        verify(rewardGrantRepository)
                .findWithFilters(eq(RewardGrantStatus.ISSUED), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("EXPIRING_SOON 정렬 - 전용 쿼리 메서드를 호출한다")
    void getGrants_expiringSoon_callsExpiringSoonMethod() {
        // given
        Page<RewardGrant> page = new PageImpl<>(List.of());
        given(
                        rewardGrantRepository.findWithFiltersOrderByExpiringSoon(
                                eq(null), eq(null), any(Pageable.class)))
                .willReturn(page);

        // when
        Page<RewardGrant> result =
                adminRewardGrantService.getGrants(
                        0, 20, null, RewardGrantSort.EXPIRING_SOON, null, null);

        // then
        assertThat(result).isEqualTo(page);
        verify(rewardGrantRepository)
                .findWithFiltersOrderByExpiringSoon(eq(null), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("phoneNumber 필터 - 전달된 전화번호로 필터링한다")
    void getGrants_withPhoneNumber_filtersByPhoneNumber() {
        // given
        Page<RewardGrant> page = new PageImpl<>(List.of());
        given(rewardGrantRepository.findWithFilters(eq(null), eq("1234"), any(Pageable.class)))
                .willReturn(page);

        // when
        Page<RewardGrant> result =
                adminRewardGrantService.getGrants(
                        0, 20, null, RewardGrantSort.LATEST, null, "1234");

        // then
        assertThat(result).isEqualTo(page);
        verify(rewardGrantRepository).findWithFilters(eq(null), eq("1234"), any(Pageable.class));
    }
}
