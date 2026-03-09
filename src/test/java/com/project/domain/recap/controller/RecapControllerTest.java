package com.project.domain.recap.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.project.domain.recap.model.MonthlyRecap;
import com.project.domain.recap.service.RecapService;
import com.project.global.auth.JwtTokenUtil;
import com.project.global.config.WebConfig;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.ExceptionAdvice;
import com.project.global.exception.code.RecapErrorCode;

@WebMvcTest(RecapController.class)
@Import({WebConfig.class, ExceptionAdvice.class})
class RecapControllerTest {

    private static final String MEMBER_TOKEN = "MEMBER_TOKEN";
    private static final Long MEMBER_ID = 101L;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RecapService recapService;
    @MockitoBean private JwtTokenUtil jwtTokenUtil;

    @Test
    @DisplayName("GET /recaps/monthly - 월간 가족 리캡을 반환한다")
    void getMonthlyRecap_returnsOk() throws Exception {
        MonthlyRecap monthlyRecap =
                new MonthlyRecap(
                        401L,
                        100L,
                        "김씨 가족",
                        LocalDate.of(2026, 3, 1),
                        53_687_091_200L,
                        107_374_182_400L,
                        new BigDecimal("50.0"),
                        new MonthlyRecap.UsageByWeekday(15.2, 18.5, 22.1, 0.0, 0.0, 20.3, 9.9),
                        new MonthlyRecap.PeakUsage(21, 23, "sunday"),
                        new MonthlyRecap.MissionSummary(10, 5, 3),
                        new MonthlyRecap.AppealSummary(4, 3, 1),
                        new MonthlyRecap.AppealHighlights(
                                new MonthlyRecap.TopSuccessfulRequester(
                                        12346L,
                                        "김민지",
                                        3,
                                        List.of(
                                                new MonthlyRecap.RecentApprovedAppeal(
                                                        91L,
                                                        12345L,
                                                        "김철수",
                                                        "야간 차단 해제를 요청했어요.",
                                                        LocalDateTime.of(2026, 3, 21, 14, 32)))),
                                new MonthlyRecap.TopAcceptedApprover(
                                        12345L,
                                        "김철수",
                                        3,
                                        List.of(
                                                new MonthlyRecap.RecentAcceptedAppeal(
                                                        91L,
                                                        12346L,
                                                        "김민지",
                                                        "야간 차단 해제를 요청했어요.",
                                                        LocalDateTime.of(2026, 3, 21, 14, 32))))),
                        new BigDecimal("82.5"),
                        LocalDateTime.of(2026, 3, 1, 0, 0));

        given(jwtTokenUtil.verify(anyString())).willReturn(null);
        given(jwtTokenUtil.getMemberId(MEMBER_TOKEN)).willReturn(MEMBER_ID);
        given(recapService.getMonthlyRecap(MEMBER_ID, 2026, 3)).willReturn(monthlyRecap);

        mockMvc.perform(
                        get("/recaps/monthly")
                                .param("year", "2026")
                                .param("month", "3")
                                .header("Authorization", "Bearer " + MEMBER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recapId").value(401L))
                .andExpect(jsonPath("$.data.familyId").value(100L))
                .andExpect(jsonPath("$.data.familyName").value("김씨 가족"))
                .andExpect(jsonPath("$.data.reportMonth").value("2026-03-01"))
                .andExpect(jsonPath("$.data.usageRatePercent").value(50.0))
                .andExpect(jsonPath("$.data.usageByWeekday.monday").value(15.2))
                .andExpect(jsonPath("$.data.peakUsage.startHour").value(21))
                .andExpect(jsonPath("$.data.missionSummary.totalMissionCount").value(10))
                .andExpect(jsonPath("$.data.appealSummary.approvedAppeals").value(3))
                .andExpect(
                        jsonPath("$.data.appealHighlights.topSuccessfulRequester.requesterName")
                                .value("김민지"))
                .andExpect(
                        jsonPath(
                                "$.data.appealHighlights.topAcceptedApprover.recentAcceptedAppeals[0].requesterName")
                                .value("김민지"))
                .andExpect(jsonPath("$.data.communicationScore").value(82.5))
                .andExpect(jsonPath("$.data.generatedAt").value("2026-03-01T00:00:00"));
    }

    @Test
    @DisplayName("GET /recaps/monthly - 리캡이 없으면 404를 반환한다")
    void getMonthlyRecap_notFound_returnsNotFound() throws Exception {
        given(jwtTokenUtil.verify(anyString())).willReturn(null);
        given(jwtTokenUtil.getMemberId(MEMBER_TOKEN)).willReturn(MEMBER_ID);
        given(recapService.getMonthlyRecap(MEMBER_ID, 2026, 3))
                .willThrow(new ApplicationException(RecapErrorCode.RECAP_NOT_FOUND));

        mockMvc.perform(
                        get("/recaps/monthly")
                                .param("year", "2026")
                                .param("month", "3")
                                .header("Authorization", "Bearer " + MEMBER_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECAP_001"));
    }
}
