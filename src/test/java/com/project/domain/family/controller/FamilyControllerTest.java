package com.project.domain.family.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.customer.enums.RoleType;
import com.project.domain.family.entity.Family;
import com.project.domain.family.service.FamilyService;
import com.project.domain.usagerecord.model.CustomerUsage;
import com.project.domain.usagerecord.model.FamilyCustomersUsage;
import com.project.domain.usagerecord.model.FamilyCustomersUsageSummary;
import com.project.domain.usagerecord.model.FamilyUsage;
import com.project.domain.usagerecord.service.UsageRecordService;
import com.project.global.auth.JwtTokenUtil;
import com.project.global.auth.aop.OwnerOnlyAspect;
import com.project.global.config.WebConfig;

@WebMvcTest(FamilyController.class)
@Import({WebConfig.class, OwnerOnlyAspect.class})
class FamilyControllerTest {

    private static final String OWNER_TOKEN = "OWNER_TOKEN";
    private static final Long OWNER_ID = 100L;
    private static final String MEMBER_TOKEN = "MEMBER_TOKEN";
    private static final Long MEMBER_ID = 101L;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private FamilyService familyService;
    @MockitoBean private UsageRecordService usageRecordService;
    @MockitoBean private JwtTokenUtil jwtTokenUtil;

    @Test
    @DisplayName("PUT /families - OWNER 권한으로 가족 이름을 수정하면 200을 반환한다")
    void updateFamilyName_ownerRole_returnsOk() throws Exception {
        Family family =
                Family.builder()
                        .name("김씨 가족")
                        .createdById(OWNER_ID)
                        .totalQuotaBytes(10_000L)
                        .usedBytes(3_000L)
                        .currentMonth(LocalDate.now().withDayOfMonth(1))
                        .build();

        given(jwtTokenUtil.verify(anyString())).willReturn(null);
        given(jwtTokenUtil.getMemberId(OWNER_TOKEN)).willReturn(OWNER_ID);
        given(jwtTokenUtil.getRole(OWNER_TOKEN)).willReturn(RoleType.OWNER);
        given(familyService.updateFamilyName(OWNER_ID, "김씨 가족")).willReturn(family);

        mockMvc.perform(
                        put("/families")
                                .header("Authorization", "Bearer " + OWNER_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new com.project.domain.family.dto.request
                                                .FamilyNameUpdateRequest("김씨 가족"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("김씨 가족"));
    }

    @Test
    @DisplayName("PUT /families - MEMBER 권한이면 403을 반환한다")
    void updateFamilyName_memberRole_returnsForbidden() throws Exception {
        given(jwtTokenUtil.verify(anyString())).willReturn(null);
        given(jwtTokenUtil.getRole(MEMBER_TOKEN)).willReturn(RoleType.MEMBER);

        mockMvc.perform(
                        put("/families")
                                .header("Authorization", "Bearer " + MEMBER_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new com.project.domain.family.dto.request
                                                .FamilyNameUpdateRequest("김씨 가족"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CUSTOMER_005"));
    }

    @Test
    @DisplayName("PUT /families - 이름이 빈 값이면 400을 반환한다")
    void updateFamilyName_blankName_returnsBadRequest() throws Exception {
        given(jwtTokenUtil.verify(anyString())).willReturn(null);
        given(jwtTokenUtil.getRole(OWNER_TOKEN)).willReturn(RoleType.OWNER);

        mockMvc.perform(
                        put("/families")
                                .header("Authorization", "Bearer " + OWNER_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

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
