package com.project.domain.recap.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.RecapErrorCode;
import com.project.domain.family.entity.Family;
import com.project.domain.family.service.FamilyService;
import com.project.domain.recap.entity.FamilyRecapMonthly;
import com.project.domain.recap.model.MonthlyRecap;
import com.project.domain.recap.repository.FamilyRecapMonthlyRepository;

@ExtendWith(MockitoExtension.class)
class RecapServiceImplTest {

    @Mock private FamilyRecapMonthlyRepository familyRecapMonthlyRepository;
    @Mock private FamilyService familyService;

    private RecapServiceImpl recapService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        recapService =
                new RecapServiceImpl(familyRecapMonthlyRepository, familyService, objectMapper);
    }

    @Test
    @DisplayName("getMonthlyRecap - 월간 리캡 스냅샷을 응답 모델로 매핑한다")
    void getMonthlyRecap_validSnapshot_returnsResponse() {
        Long customerId = 101L;
        Long familyId = 100L;
        LocalDate reportMonth = LocalDate.of(2026, 3, 1);

        FamilyRecapMonthly recap =
                FamilyRecapMonthly.builder()
                        .id(401L)
                        .familyId(familyId)
                        .reportMonth(reportMonth)
                        .totalUsedBytes(53_687_091_200L)
                        .totalQuotaBytes(107_374_182_400L)
                        .usageRatePercent(new BigDecimal("50.0"))
                        .usageByWeekday(
                                """
                                {
                                  "monday": 15.2,
                                  "tuesday": 18.5,
                                  "wednesday": 22.1,
                                  "thursday": 0.0,
                                  "friday": 0.0,
                                  "saturday": 20.3,
                                  "sunday": 9.9
                                }
                                """)
                        .peakUsage(
                                """
                                {
                                  "startHour": 21,
                                  "endHour": 23,
                                  "mostUsedWeekday": "sunday"
                                }
                                """)
                        .missionSummaryJson(
                                """
                                {
                                  "totalMissionCount": 10,
                                  "completedMissionCount": 5,
                                  "rejectedRequestCount": 3
                                }
                                """)
                        .appealSummaryJson(
                                """
                                {
                                  "totalAppeals": 4,
                                  "approvedAppeals": 3,
                                  "rejectedAppeals": 1
                                }
                                """)
                        .appealHighlightsJson(
                                """
                                {
                                  "topSuccessfulRequester": {
                                    "requesterId": 12346,
                                    "requesterName": "김민지",
                                    "approvedAppealCount": 3,
                                    "recentApprovedAppeals": [
                                      {
                                        "appealId": 91,
                                        "approverId": 12345,
                                        "approverName": "김철수",
                                        "requestReason": "야간 차단 해제를 요청했어요.",
                                        "requestedAt": "2026-03-21T14:32:00"
                                      }
                                    ]
                                  },
                                  "topAcceptedApprover": {
                                    "approverId": 12345,
                                    "approverName": "김철수",
                                    "approvedAppealCount": 3,
                                    "recentAcceptedAppeals": [
                                      {
                                        "appealId": 91,
                                        "requesterId": 12346,
                                        "requesterName": "김민지",
                                        "requestReason": "야간 차단 해제를 요청했어요.",
                                        "resolvedAt": "2026-03-21T14:32:00"
                                      }
                                    ]
                                  }
                                }
                                """)
                        .communicationScore(new BigDecimal("82.5"))
                        .build();
        ReflectionTestUtils.setField(recap, "createdAt", LocalDateTime.of(2026, 3, 1, 0, 0));
        ReflectionTestUtils.setField(recap, "updatedAt", LocalDateTime.of(2026, 3, 2, 8, 30));

        Family family =
                Family.builder()
                        .id(familyId)
                        .name("김씨 가족")
                        .createdById(1L)
                        .totalQuotaBytes(107_374_182_400L)
                        .usedBytes(53_687_091_200L)
                        .currentMonth(reportMonth)
                        .build();

        given(familyService.getFamilyIdByCustomerId(customerId)).willReturn(familyId);
        given(
                        familyRecapMonthlyRepository.findByFamilyIdAndReportMonthAndDeletedAtIsNull(
                                familyId, reportMonth))
                .willReturn(Optional.of(recap));
        given(familyService.getFamilyById(familyId)).willReturn(family);

        MonthlyRecap response = recapService.getMonthlyRecap(customerId, 2026, 3);

        assertThat(response.recapId()).isEqualTo(401L);
        assertThat(response.familyId()).isEqualTo(familyId);
        assertThat(response.familyName()).isEqualTo("김씨 가족");
        assertThat(response.reportMonth()).isEqualTo(reportMonth);
        assertThat(response.usageRatePercent()).isEqualByComparingTo("50.0");
        assertThat(response.usageByWeekday().wednesday()).isEqualTo(22.1);
        assertThat(response.peakUsage().mostUsedWeekday()).isEqualTo("sunday");
        assertThat(response.missionSummary().completedMissionCount()).isEqualTo(5);
        assertThat(response.appealSummary().approvedAppeals()).isEqualTo(3);
        assertThat(response.appealHighlights().topSuccessfulRequester().requesterName())
                .isEqualTo("김민지");
        assertThat(
                        response.appealHighlights()
                                .topAcceptedApprover()
                                .recentAcceptedAppeals()
                                .get(0)
                                .requesterName())
                .isEqualTo("김민지");
        assertThat(response.communicationScore()).isEqualByComparingTo("82.5");
        assertThat(response.generatedAt()).isEqualTo(LocalDateTime.of(2026, 3, 2, 8, 30));
    }

    @Test
    @DisplayName("getMonthlyRecap - JSON 스냅샷이 비어 있으면 기본값을 반환한다")
    void getMonthlyRecap_emptySnapshots_returnsDefaultValues() {
        Long customerId = 101L;
        Long familyId = 100L;
        LocalDate reportMonth = LocalDate.of(2026, 3, 1);

        FamilyRecapMonthly recap =
                FamilyRecapMonthly.builder()
                        .id(402L)
                        .familyId(familyId)
                        .reportMonth(reportMonth)
                        .totalUsedBytes(10_000L)
                        .totalQuotaBytes(20_000L)
                        .usageRatePercent(new BigDecimal("50.0"))
                        .usageByWeekday(null)
                        .peakUsage(null)
                        .missionSummaryJson("")
                        .appealSummaryJson(null)
                        .appealHighlightsJson(null)
                        .communicationScore(null)
                        .build();
        ReflectionTestUtils.setField(recap, "createdAt", LocalDateTime.of(2026, 3, 1, 0, 0));

        Family family =
                Family.builder()
                        .id(familyId)
                        .name("김씨 가족")
                        .createdById(1L)
                        .totalQuotaBytes(20_000L)
                        .usedBytes(10_000L)
                        .currentMonth(reportMonth)
                        .build();

        given(familyService.getFamilyIdByCustomerId(customerId)).willReturn(familyId);
        given(
                        familyRecapMonthlyRepository.findByFamilyIdAndReportMonthAndDeletedAtIsNull(
                                familyId, reportMonth))
                .willReturn(Optional.of(recap));
        given(familyService.getFamilyById(familyId)).willReturn(family);

        MonthlyRecap response = recapService.getMonthlyRecap(customerId, 2026, 3);

        assertThat(response.usageByWeekday().monday()).isEqualTo(0.0);
        assertThat(response.peakUsage().startHour()).isNull();
        assertThat(response.missionSummary().totalMissionCount()).isZero();
        assertThat(response.appealSummary().approvedAppeals()).isZero();
        assertThat(response.appealHighlights().topSuccessfulRequester().requesterId()).isNull();
        assertThat(response.appealHighlights().topSuccessfulRequester().approvedAppealCount())
                .isZero();
        assertThat(response.appealHighlights().topAcceptedApprover().recentAcceptedAppeals())
                .isEmpty();
        assertThat(response.generatedAt()).isEqualTo(LocalDateTime.of(2026, 3, 1, 0, 0));
    }

    @Test
    @DisplayName("getMonthlyRecap - 리캡이 없으면 예외를 던진다")
    void getMonthlyRecap_notFound_throwsException() {
        Long customerId = 101L;
        Long familyId = 100L;
        LocalDate reportMonth = LocalDate.of(2026, 3, 1);

        given(familyService.getFamilyIdByCustomerId(customerId)).willReturn(familyId);
        given(
                        familyRecapMonthlyRepository.findByFamilyIdAndReportMonthAndDeletedAtIsNull(
                                familyId, reportMonth))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> recapService.getMonthlyRecap(customerId, 2026, 3))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        exception ->
                                assertThat(((ApplicationException) exception).getCode())
                                        .isEqualTo(RecapErrorCode.RECAP_NOT_FOUND));
    }

    @Test
    @DisplayName("getMonthlyRecap - JSON 파싱에 실패하면 리캡 전용 예외를 던진다")
    void getMonthlyRecap_invalidJson_throwsRecapError() {
        Long customerId = 101L;
        Long familyId = 100L;
        LocalDate reportMonth = LocalDate.of(2026, 3, 1);

        FamilyRecapMonthly recap =
                FamilyRecapMonthly.builder()
                        .id(403L)
                        .familyId(familyId)
                        .reportMonth(reportMonth)
                        .totalUsedBytes(10_000L)
                        .totalQuotaBytes(20_000L)
                        .usageRatePercent(new BigDecimal("50.0"))
                        .usageByWeekday("{invalid-json}")
                        .build();

        Family family =
                Family.builder()
                        .id(familyId)
                        .name("김씨 가족")
                        .createdById(1L)
                        .totalQuotaBytes(20_000L)
                        .usedBytes(10_000L)
                        .currentMonth(reportMonth)
                        .build();

        given(familyService.getFamilyIdByCustomerId(customerId)).willReturn(familyId);
        given(
                        familyRecapMonthlyRepository.findByFamilyIdAndReportMonthAndDeletedAtIsNull(
                                familyId, reportMonth))
                .willReturn(Optional.of(recap));
        given(familyService.getFamilyById(familyId)).willReturn(family);

        assertThatThrownBy(() -> recapService.getMonthlyRecap(customerId, 2026, 3))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        exception ->
                                assertThat(((ApplicationException) exception).getCode())
                                        .isEqualTo(
                                                RecapErrorCode.RECAP_JSON_DESERIALIZATION_FAILED));
    }
}
