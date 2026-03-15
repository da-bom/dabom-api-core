package com.project.domain.usagerecord.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.domain.family.entity.Family;
import com.project.domain.family.entity.FamilyQuota;
import com.project.domain.family.repository.FamilyQuotaRepository;
import com.project.domain.family.repository.projection.FamilyUsageCustomerRow;
import com.project.domain.family.service.FamilyService;
import com.project.domain.usagerecord.model.FamilyCustomersUsage;
import com.project.domain.usagerecord.model.FamilyUsage;

@ExtendWith(MockitoExtension.class)
class UsageRecordServiceImplTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-03-14T15:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDate CURRENT_MONTH = LocalDate.of(2026, 3, 1);

    @Mock private FamilyService familyService;
    @Mock private FamilyQuotaRepository familyQuotaRepository;

    private UsageRecordServiceImpl usageRecordService;

    @BeforeEach
    void setUp() {
        usageRecordService =
                new UsageRecordServiceImpl(familyService, familyQuotaRepository, FIXED_CLOCK);
    }

    @Test
    @DisplayName("getCurrentFamilyUsage - 현재월 family_quota를 읽는다")
    void getCurrentFamilyUsage_readsCurrentMonthFamilyQuota() {
        Long customerId = 10L;
        Long familyId = 100L;
        Family family = Family.builder().id(familyId).name("다봄 가족").createdById(1L).build();
        FamilyQuota familyQuota =
                FamilyQuota.builder()
                        .familyId(familyId)
                        .currentMonth(CURRENT_MONTH)
                        .totalQuotaBytes(10_000L)
                        .usedBytes(2_500L)
                        .build();

        given(familyService.getFamilyIdByCustomerId(customerId)).willReturn(familyId);
        given(familyService.getFamilyById(familyId)).willReturn(family);
        given(familyQuotaRepository.findActiveByFamilyIdAndCurrentMonth(familyId, CURRENT_MONTH))
                .willReturn(Optional.of(familyQuota));

        FamilyUsage actual = usageRecordService.getCurrentFamilyUsage(customerId);

        assertThat(actual.familyId()).isEqualTo(familyId);
        assertThat(actual.familyName()).isEqualTo("다봄 가족");
        assertThat(actual.totalQuotaBytes()).isEqualTo(10_000L);
        assertThat(actual.totalUsedBytes()).isEqualTo(2_500L);
    }

    @Test
    @DisplayName("getCustomersUsageReport - family_quota 총량으로 remaining과 사용률을 계산한다")
    void getCustomersUsageReport_usesFamilyQuotaTotalQuota() {
        Long customerId = 10L;
        Long familyId = 100L;
        Family family = Family.builder().id(familyId).name("다봄 가족").createdById(1L).build();
        FamilyQuota familyQuota =
                FamilyQuota.builder()
                        .familyId(familyId)
                        .currentMonth(CURRENT_MONTH)
                        .totalQuotaBytes(10_000L)
                        .usedBytes(2_500L)
                        .build();
        List<FamilyUsageCustomerRow> rows =
                List.of(
                        new FamilyUsageCustomerRow(10L, "아빠", 1_200L, 5_000L, false, null),
                        new FamilyUsageCustomerRow(11L, "엄마", 800L, 3_000L, false, null),
                        new FamilyUsageCustomerRow(12L, "아이", 500L, 2_000L, false, null));

        given(familyService.getFamilyIdByCustomerId(customerId)).willReturn(familyId);
        given(familyService.getFamilyById(familyId)).willReturn(family);
        given(familyService.getUsageReportCustomers(familyId, CURRENT_MONTH)).willReturn(rows);
        given(familyQuotaRepository.findActiveByFamilyIdAndCurrentMonth(familyId, CURRENT_MONTH))
                .willReturn(Optional.of(familyQuota));

        FamilyCustomersUsage actual =
                usageRecordService.getCustomersUsageReport(customerId, 2026, 3);

        assertThat(actual.totalQuotaBytes()).isEqualTo(10_000L);
        assertThat(actual.remainingBytes()).isEqualTo(7_500L);
        assertThat(actual.usedPercent()).isEqualTo(25.0);
        assertThat(actual.customers()).hasSize(3);
    }
}
