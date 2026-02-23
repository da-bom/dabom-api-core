package com.project.domain.family.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.customer.enums.RoleType;
import com.project.domain.family.dto.request.FamilySearchRequest;
import com.project.domain.family.infra.cache.FamilyCacheRepository;
import com.project.domain.family.support.FamilyApiTestSupport;
import com.project.global.auth.JwtTokenUtil;

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=localhost:9092")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FamilyControllerTest {

    private static final String ADMIN_TOKEN = "ADMIN";
    private static final String MEMBER_TOKEN = "MEMBER";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private FamilyApiTestSupport familyApiTestSupport;

    @MockitoBean private JwtTokenUtil jwtTokenUtil;
    @MockitoBean private FamilyCacheRepository familyCacheRepository;

    @Test
    @DisplayName("POST /families - 가족 검색 결과를 반환한다")
    void searchFamilies_validRequest_returnsFamilyList() throws Exception {
        // given
        FamilyApiTestSupport.FamilyContext familyContext =
                familyApiTestSupport.buildFamilyContext();

        FamilySearchRequest request =
                new FamilySearchRequest(
                        0,
                        20,
                        new FamilySearchRequest.Filters(
                                new FamilySearchRequest.StringCondition("contains", "아빠"),
                                null,
                                null),
                        List.of(new FamilySearchRequest.SortCondition("createdAt", "desc")));

        given(jwtTokenUtil.getRole(ADMIN_TOKEN)).willReturn(RoleType.ADMIN);

        // when
        MvcResult mvcResult =
                mockMvc.perform(
                                post("/families")
                                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andReturn();

        // then
        JsonNode data =
                objectMapper.readTree(mvcResult.getResponse().getContentAsString()).path("data");

        assertThat(data.path("content").size()).isEqualTo(1);
        assertThat(data.path("content").get(0).path("familyId").asLong())
                .isEqualTo(familyContext.family().getId());
        assertThat(data.path("content").get(0).path("familyName").asText()).isEqualTo("다봄 가족");
        assertThat(data.path("content").get(0).path("customers").size()).isEqualTo(3);
    }

    @Test
    @DisplayName("GET /families/{familyId} - 가족 상세 조회 결과를 반환한다")
    void getFamilyDetail_validFamilyId_returnsFamilyDetail() throws Exception {
        // given
        FamilyApiTestSupport.FamilyContext familyContext =
                familyApiTestSupport.buildFamilyContext();
        familyApiTestSupport.buildQuotas(familyContext, 1_200L, 800L, 500L);

        given(jwtTokenUtil.getRole(ADMIN_TOKEN)).willReturn(RoleType.ADMIN);
        given(
                        familyCacheRepository.findCustomerMonthlyUsageBytes(
                                familyContext.family().getId(), familyContext.dad().getId()))
                .willReturn(Optional.empty());
        given(
                        familyCacheRepository.findCustomerMonthlyUsageBytes(
                                familyContext.family().getId(), familyContext.mom().getId()))
                .willReturn(Optional.empty());
        given(
                        familyCacheRepository.findCustomerMonthlyUsageBytes(
                                familyContext.family().getId(), familyContext.kid().getId()))
                .willReturn(Optional.empty());

        // when
        MvcResult mvcResult =
                mockMvc.perform(
                                get("/families/{familyId}", familyContext.family().getId())
                                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                        .andExpect(status().isOk())
                        .andReturn();

        // then
        JsonNode data =
                objectMapper.readTree(mvcResult.getResponse().getContentAsString()).path("data");

        assertThat(data.path("familyId").asLong()).isEqualTo(familyContext.family().getId());
        assertThat(data.path("familyName").asText()).isEqualTo("다봄 가족");
        assertThat(data.path("customers").size()).isEqualTo(3);
        assertThat(data.path("totalQuotaBytes").asLong()).isEqualTo(10_000L);
        assertThat(data.path("usedBytes").asLong()).isEqualTo(2_500L);
        assertThat(data.path("usedPercent").asDouble()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("POST /families - 잘못된 검색 조건이면 400을 반환한다")
    void searchFamilies_invalidCondition_returnsBadRequest() throws Exception {
        // given
        FamilySearchRequest request =
                new FamilySearchRequest(
                        0,
                        20,
                        null,
                        List.of(new FamilySearchRequest.SortCondition("createdAt", "invalid")));
        given(jwtTokenUtil.getRole(ADMIN_TOKEN)).willReturn(RoleType.ADMIN);

        // when & then
        mockMvc.perform(
                        post("/families")
                                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FAMILY_002"));
    }

    @Test
    @DisplayName("GET /families/{familyId} - 존재하지 않는 가족이면 404를 반환한다")
    void getFamilyDetail_notFoundFamily_returnsNotFound() throws Exception {
        // given
        given(jwtTokenUtil.getRole(ADMIN_TOKEN)).willReturn(RoleType.ADMIN);

        // when & then
        mockMvc.perform(
                        get("/families/{familyId}", 999_999L)
                                .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FAMILY_001"));
    }

    @Test
    @DisplayName("GET /families/{familyId} - 관리자 권한이 아니면 403을 반환한다")
    void getFamilyDetail_nonAdminRole_returnsForbidden() throws Exception {
        // given
        FamilyApiTestSupport.FamilyContext familyContext =
                familyApiTestSupport.buildFamilyContext();
        given(jwtTokenUtil.getRole(MEMBER_TOKEN)).willReturn(RoleType.MEMBER);

        // when & then
        mockMvc.perform(
                        get("/families/{familyId}", familyContext.family().getId())
                                .header("Authorization", "Bearer " + MEMBER_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_003"));
    }
}
