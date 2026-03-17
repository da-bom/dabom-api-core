package com.project.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;

import com.project.domain.admin.model.AdminDashboard;
import com.project.domain.customer.entity.CustomerQuota;
import com.project.domain.customer.repository.CustomerQuotaRepository;
import com.project.domain.customer.repository.CustomerRepository;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.family.repository.FamilyRepository;
import com.project.domain.usagerecord.repository.UsageRecordRepository;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-03-17T01:30:00Z"), ZONE);
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 17);
    private static final LocalDate CURRENT_MONTH = LocalDate.of(2026, 3, 1);
    private static final LocalDateTime TODAY_START = LocalDateTime.of(TODAY, LocalTime.MIN);
    private static final LocalDateTime NOW = LocalDateTime.now(FIXED_CLOCK);
    private static final LocalDateTime ONE_MINUTE_AGO = NOW.minusSeconds(60);

    @Mock private FamilyRepository familyRepository;
    @Mock private FamilyMemberRepository familyMemberRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerQuotaRepository customerQuotaRepository;
    @Mock private UsageRecordRepository usageRecordRepository;
    @Mock private HealthEndpoint healthEndpoint;

    private AdminDashboardServiceImpl adminDashboardService;

    @BeforeEach
    void setUp() {
        adminDashboardService =
                new AdminDashboardServiceImpl(
                        familyRepository,
                        familyMemberRepository,
                        customerRepository,
                        customerQuotaRepository,
                        usageRecordRepository,
                        healthEndpoint,
                        FIXED_CLOCK);
    }

    @Test
    @DisplayName("getDashboard - 시스템 전체 통계를 정상적으로 집계한다")
    void getDashboard_returnsAggregatedStats() {
        // given
        given(familyRepository.countByDeletedAtIsNull()).willReturn(250000L);
        given(familyMemberRepository.countDistinctActiveFamilies()).willReturn(248500L);
        given(customerRepository.countByDeletedAtIsNull()).willReturn(1000000L);
        given(
                        customerQuotaRepository
                                .countByIsBlockedTrueAndCurrentMonthAndDeletedAtIsNull(
                                        CURRENT_MONTH))
                .willReturn(1523L);
        given(usageRecordRepository.countByEventTimeGreaterThanEqualAndDeletedAtIsNull(TODAY_START))
                .willReturn(432000L);
        given(
                        usageRecordRepository.countByEventTimeGreaterThanEqualAndDeletedAtIsNull(
                                ONE_MINUTE_AGO))
                .willReturn(300L);
        mockHealthEndpoint("UP", "UP", "UP");
        given(
                        customerQuotaRepository
                                .findTop10ByIsBlockedTrueAndCurrentMonthAndDeletedAtIsNullOrderByUpdatedAtDesc(
                                        CURRENT_MONTH))
                .willReturn(List.of());

        // when
        AdminDashboard result = adminDashboardService.getDashboard();

        // then
        assertThat(result.totalFamilies()).isEqualTo(250000L);
        assertThat(result.activeFamilies()).isEqualTo(248500L);
        assertThat(result.totalUsers()).isEqualTo(1000000L);
        assertThat(result.blockedUsers()).isEqualTo(1523L);
        assertThat(result.todayEvents()).isEqualTo(432000L);
        assertThat(result.currentTps()).isEqualTo(5L);
        assertThat(result.systemHealth().redis()).isEqualTo("UP");
        assertThat(result.systemHealth().kafka()).isEqualTo("UP");
        assertThat(result.systemHealth().mysql()).isEqualTo("UP");
        assertThat(result.recentBlocks()).isEmpty();
    }

    @Test
    @DisplayName("getDashboard - recentBlocks를 CustomerQuota에서 올바르게 변환한다")
    void getDashboard_mapsRecentBlocksCorrectly() {
        // given
        stubCountQueries();
        mockHealthEndpoint("UP", "UP", "UP");

        LocalDateTime blockedTime = LocalDateTime.of(2026, 3, 15, 10, 30, 0);
        CustomerQuota blocked =
                CustomerQuota.builder()
                        .customerId(12346L)
                        .familyId(100L)
                        .monthlyLimitBytes(10_000_000L)
                        .monthlyUsedBytes(10_000_000L)
                        .currentMonth(CURRENT_MONTH)
                        .isBlocked(true)
                        .blockReason("MONTHLY_LIMIT_EXCEEDED")
                        .build();
        // updatedAt은 BaseEntity의 JPA Auditing으로 설정되므로 테스트에서는 null

        given(
                        customerQuotaRepository
                                .findTop10ByIsBlockedTrueAndCurrentMonthAndDeletedAtIsNullOrderByUpdatedAtDesc(
                                        CURRENT_MONTH))
                .willReturn(List.of(blocked));

        // when
        AdminDashboard result = adminDashboardService.getDashboard();

        // then
        assertThat(result.recentBlocks()).hasSize(1);
        AdminDashboard.RecentBlock block = result.recentBlocks().get(0);
        assertThat(block.familyId()).isEqualTo(100L);
        assertThat(block.customerId()).isEqualTo(12346L);
        assertThat(block.reason()).isEqualTo("MONTHLY_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("getDashboard - 헬스 컴포넌트가 없으면 UNKNOWN을 반환한다")
    void getDashboard_missingHealthComponent_returnsUnknown() {
        // given
        stubCountQueries();
        given(
                        customerQuotaRepository
                                .findTop10ByIsBlockedTrueAndCurrentMonthAndDeletedAtIsNullOrderByUpdatedAtDesc(
                                        CURRENT_MONTH))
                .willReturn(List.of());

        // redis만 존재, kafka/db 없음
        org.springframework.boot.actuate.health.SystemHealth systemHealth =
                mock(org.springframework.boot.actuate.health.SystemHealth.class);
        Map<String, HealthComponent> components = new HashMap<>();
        components.put("redis", Health.up().build());
        given(systemHealth.getComponents()).willReturn(components);
        given(healthEndpoint.health()).willReturn(systemHealth);

        // when
        AdminDashboard result = adminDashboardService.getDashboard();

        // then
        assertThat(result.systemHealth().redis()).isEqualTo("UP");
        assertThat(result.systemHealth().kafka()).isEqualTo("UNKNOWN");
        assertThat(result.systemHealth().mysql()).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("getDashboard - 헬스 컴포넌트가 DOWN이면 DOWN을 반환한다")
    void getDashboard_downHealthComponent_returnsDown() {
        // given
        stubCountQueries();
        given(
                        customerQuotaRepository
                                .findTop10ByIsBlockedTrueAndCurrentMonthAndDeletedAtIsNullOrderByUpdatedAtDesc(
                                        CURRENT_MONTH))
                .willReturn(List.of());

        mockHealthEndpoint("DOWN", "UP", "UP");

        // when
        AdminDashboard result = adminDashboardService.getDashboard();

        // then
        assertThat(result.systemHealth().redis()).isEqualTo("DOWN");
        assertThat(result.systemHealth().kafka()).isEqualTo("UP");
        assertThat(result.systemHealth().mysql()).isEqualTo("UP");
    }

    @Test
    @DisplayName("getDashboard - 최근 1분 이벤트가 60 미만이면 currentTps는 0이다")
    void getDashboard_lowEventCount_tpsIsZero() {
        // given
        given(familyRepository.countByDeletedAtIsNull()).willReturn(10L);
        given(familyMemberRepository.countDistinctActiveFamilies()).willReturn(10L);
        given(customerRepository.countByDeletedAtIsNull()).willReturn(50L);
        given(
                        customerQuotaRepository
                                .countByIsBlockedTrueAndCurrentMonthAndDeletedAtIsNull(
                                        CURRENT_MONTH))
                .willReturn(0L);
        given(usageRecordRepository.countByEventTimeGreaterThanEqualAndDeletedAtIsNull(TODAY_START))
                .willReturn(100L);
        given(
                        usageRecordRepository.countByEventTimeGreaterThanEqualAndDeletedAtIsNull(
                                ONE_MINUTE_AGO))
                .willReturn(30L);
        mockHealthEndpoint("UP", "UP", "UP");
        given(
                        customerQuotaRepository
                                .findTop10ByIsBlockedTrueAndCurrentMonthAndDeletedAtIsNullOrderByUpdatedAtDesc(
                                        CURRENT_MONTH))
                .willReturn(List.of());

        // when
        AdminDashboard result = adminDashboardService.getDashboard();

        // then
        assertThat(result.todayEvents()).isEqualTo(100L);
        assertThat(result.currentTps()).isEqualTo(0L);
    }

    private void stubCountQueries() {
        given(familyRepository.countByDeletedAtIsNull()).willReturn(100L);
        given(familyMemberRepository.countDistinctActiveFamilies()).willReturn(90L);
        given(customerRepository.countByDeletedAtIsNull()).willReturn(500L);
        given(
                        customerQuotaRepository
                                .countByIsBlockedTrueAndCurrentMonthAndDeletedAtIsNull(
                                        CURRENT_MONTH))
                .willReturn(5L);
        given(usageRecordRepository.countByEventTimeGreaterThanEqualAndDeletedAtIsNull(TODAY_START))
                .willReturn(1000L);
        given(
                        usageRecordRepository.countByEventTimeGreaterThanEqualAndDeletedAtIsNull(
                                ONE_MINUTE_AGO))
                .willReturn(60L);
    }

    private void mockHealthEndpoint(String redisStatus, String kafkaStatus, String dbStatus) {
        org.springframework.boot.actuate.health.SystemHealth systemHealth =
                mock(org.springframework.boot.actuate.health.SystemHealth.class);
        Map<String, HealthComponent> components = new HashMap<>();
        components.put("redis", Health.status(redisStatus).build());
        components.put("kafka", Health.status(kafkaStatus).build());
        components.put("db", Health.status(dbStatus).build());
        given(systemHealth.getComponents()).willReturn(components);
        given(healthEndpoint.health()).willReturn(systemHealth);
    }
}
