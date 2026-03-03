package com.project.domain.family.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.project.domain.usagerecord.model.CustomerUsage;
import com.project.domain.usagerecord.model.FamilyCustomersUsage;
import com.project.domain.usagerecord.model.FamilyCustomersUsageSummary;
import com.project.domain.usagerecord.model.FamilyUsage;
import com.project.domain.usagerecord.service.UsageRecordService;
import com.project.global.auth.JwtTokenUtil;
import com.project.global.config.WebConfig;

@WebMvcTest(FamilyController.class)
@Import(WebConfig.class)
class FamilyControllerTest {

    private static final String MEMBER_TOKEN = "MEMBER_TOKEN";
    private static final Long MEMBER_ID = 101L;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UsageRecordService usageRecordService;
    @MockitoBean private JwtTokenUtil jwtTokenUtil;

    @Test
    @DisplayName("GET /families/usage/current - 가족 현재 사용량을 반환한다")
    void getCurrentFamilyUsageReturnsOk() throws Exception {
        FamilyUsage familyUsage = new FamilyUsage(1L, "테스트 가족", 100_000L, 40_000L);

        given(jwtTokenUtil.verify(anyString())).willReturn(null);
        given(jwtTokenUtil.getMemberId(MEMBER_TOKEN)).willReturn(MEMBER_ID);
        given(usageRecordService.getCurrentFamilyUsage(MEMBER_ID)).willReturn(familyUsage);

        mockMvc.perform(
                        get("/families/usage/current")
                                .header("Authorization", "Bearer " + MEMBER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.familyId").value(1L))
                .andExpect(jsonPath("$.data.familyName").value("테스트 가족"))
                .andExpect(jsonPath("$.data.totalQuotaBytes").value(100000))
                .andExpect(jsonPath("$.data.totalUsedBytes").value(40000));
    }

    @Test
    @DisplayName("GET /families/usage/customers - 월별 구성원 사용량 목록을 반환한다")
    void getCustomersUsageReturnsOk() throws Exception {
        CustomerUsage me = new CustomerUsage(101L, "다봄1", 12_000L, 50_000L, false, null, true);
        CustomerUsage other =
                new CustomerUsage(102L, "다봄2", 30_000L, 50_000L, true, "TIME_POLICY", false);
        FamilyCustomersUsageSummary usageSummary =
                new FamilyCustomersUsageSummary(1L, 2026, 2, List.of(me, other));

        given(jwtTokenUtil.verify(anyString())).willReturn(null);
        given(jwtTokenUtil.getMemberId(MEMBER_TOKEN)).willReturn(MEMBER_ID);
        given(usageRecordService.getCustomersUsageSummaryReport(MEMBER_ID, 2026, 2))
                .willReturn(usageSummary);

        mockMvc.perform(
                        get("/families/usage/customers")
                                .param("year", "2026")
                                .param("month", "2")
                                .header("Authorization", "Bearer " + MEMBER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.familyId").value(1L))
                .andExpect(jsonPath("$.data.year").value(2026))
                .andExpect(jsonPath("$.data.month").value(2))
                .andExpect(jsonPath("$.data.familyName").doesNotExist())
                .andExpect(jsonPath("$.data.totalQuotaBytes").doesNotExist())
                .andExpect(jsonPath("$.data.remainingBytes").doesNotExist())
                .andExpect(jsonPath("$.data.usedPercent").doesNotExist())
                .andExpect(jsonPath("$.data.customers[0].customerId").value(101L))
                .andExpect(jsonPath("$.data.customers[0].isMe").value(true))
                .andExpect(jsonPath("$.data.customers[1].customerId").value(102L))
                .andExpect(jsonPath("$.data.customers[1].isBlocked").value(true))
                .andExpect(jsonPath("$.data.customers[1].blockReason").value("TIME_POLICY"));
    }

    @Test
    @DisplayName("GET /families/usage/dashboard returns dashboard usage response")
    void getCustomersUsageDashboardReturnsOk() throws Exception {
        CustomerUsage me = new CustomerUsage(101L, "다봄1", 12_000L, 50_000L, false, null, true);
        CustomerUsage other =
                new CustomerUsage(102L, "다봄2", 30_000L, 50_000L, true, "TIME_POLICY", false);
        FamilyCustomersUsage usageReport =
                new FamilyCustomersUsage(
                        1L, "다봄가족", 2026, 2, 100_000L, 58_000L, 42.0, List.of(me, other));

        given(jwtTokenUtil.verify(anyString())).willReturn(null);
        given(jwtTokenUtil.getMemberId(MEMBER_TOKEN)).willReturn(MEMBER_ID);
        given(usageRecordService.getCustomersUsageReport(MEMBER_ID, 2026, 2))
                .willReturn(usageReport);

        mockMvc.perform(
                        get("/families/usage/dashboard")
                                .param("year", "2026")
                                .param("month", "2")
                                .header("Authorization", "Bearer " + MEMBER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.familyId").value(1L))
                .andExpect(jsonPath("$.data.familyName").value("다봄가족"))
                .andExpect(jsonPath("$.data.year").value(2026))
                .andExpect(jsonPath("$.data.month").value(2))
                .andExpect(jsonPath("$.data.totalQuotaBytes").value(100000))
                .andExpect(jsonPath("$.data.remainingBytes").value(58000))
                .andExpect(jsonPath("$.data.usedPercent").value(42.0))
                .andExpect(jsonPath("$.data.customers[0].customerId").value(101L))
                .andExpect(jsonPath("$.data.customers[0].isMe").value(true))
                .andExpect(jsonPath("$.data.customers[1].customerId").value(102L))
                .andExpect(jsonPath("$.data.customers[1].isBlocked").value(true))
                .andExpect(jsonPath("$.data.customers[1].blockReason").value("TIME_POLICY"));
    }
}
