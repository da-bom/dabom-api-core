package com.project.domain.reward.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.customer.entity.Customer;
import com.project.domain.customer.enums.RoleType;
import com.project.domain.customer.repository.CustomerRepository;
import com.project.domain.family.entity.Family;
import com.project.domain.family.entity.FamilyMember;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.family.repository.FamilyRepository;
import com.project.domain.mission.entity.MissionItem;
import com.project.domain.mission.entity.MissionRequest;
import com.project.domain.mission.enums.MissionRequestStatus;
import com.project.domain.mission.enums.MissionStatus;
import com.project.domain.mission.repository.MissionItemRepository;
import com.project.domain.mission.repository.MissionRequestRepository;
import com.project.domain.reward.entity.Reward;
import com.project.domain.reward.entity.RewardTemplate;
import com.project.domain.reward.enums.RewardCategory;
import com.project.domain.reward.repository.RewardRepository;
import com.project.domain.reward.repository.RewardTemplateRepository;
import com.project.global.auth.JwtTokenUtil;

import io.jsonwebtoken.Claims;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@EmbeddedKafka(
        partitions = 1,
        topics = {"usage-events"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
class RewardControllerIntegrationTest {

    private static final String OWNER_TOKEN = "OWNER_TOKEN";
    private static final String MEMBER_TOKEN = "MEMBER_TOKEN";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private CustomerRepository customerRepository;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private FamilyMemberRepository familyMemberRepository;
    @Autowired private MissionItemRepository missionItemRepository;
    @Autowired private MissionRequestRepository missionRequestRepository;
    @Autowired private RewardTemplateRepository rewardTemplateRepository;
    @Autowired private RewardRepository rewardRepository;

    @MockitoBean private JwtTokenUtil jwtTokenUtil;
    @MockitoBean private KafkaTemplate<String, String> kafkaTemplate;

    private Customer owner;
    private Customer member;
    private Family family;
    private RewardTemplate rewardTemplate;
    private MissionItem mission;

    @BeforeEach
    void setUp() {
        owner = customerRepository.save(new Customer("01000000001", "pw", "owner"));
        member = customerRepository.save(new Customer("01000000002", "pw", "member"));

        family =
                familyRepository.save(
                        Family.builder()
                                .name("family")
                                .createdById(owner.getId())
                                .totalQuotaBytes(10_000L)
                                .usedBytes(0L)
                                .currentMonth(LocalDate.now().withDayOfMonth(1))
                                .build());
        familyMemberRepository.save(
                FamilyMember.builder()
                        .familyId(family.getId())
                        .customerId(owner.getId())
                        .role(RoleType.OWNER)
                        .build());
        familyMemberRepository.save(
                FamilyMember.builder()
                        .familyId(family.getId())
                        .customerId(member.getId())
                        .role(RoleType.MEMBER)
                        .build());

        rewardTemplate =
                rewardTemplateRepository.save(
                        RewardTemplate.builder()
                                .name("data reward")
                                .category(RewardCategory.DATA)
                                .price(5000)
                                .isSystem(true)
                                .isActive(true)
                                .build());

        Reward reward =
                rewardRepository.save(
                        Reward.builder()
                                .rewardTemplate(rewardTemplate)
                                .name("data reward")
                                .category(RewardCategory.DATA)
                                .thumbnailUrl("/rewards/data.jpg")
                                .build());

        mission =
                missionItemRepository.save(
                        MissionItem.builder()
                                .familyId(family.getId())
                                .targetCustomerId(member.getId())
                                .createdById(owner.getId())
                                .reward(reward)
                                .missionText("clean room")
                                .status(MissionStatus.ACTIVE)
                                .build());

        Claims ownerClaims = mock(Claims.class);
        doReturn(owner.getId().toString()).when(ownerClaims).getSubject();
        doReturn(RoleType.OWNER.name()).when(ownerClaims).get("role", String.class);

        Claims memberClaims = mock(Claims.class);
        doReturn(member.getId().toString()).when(memberClaims).getSubject();
        doReturn(RoleType.MEMBER.name()).when(memberClaims).get("role", String.class);

        lenient().when(jwtTokenUtil.getVerifiedClaims(OWNER_TOKEN)).thenReturn(ownerClaims);
        lenient().when(jwtTokenUtil.getVerifiedClaims(MEMBER_TOKEN)).thenReturn(memberClaims);
    }

    @Test
    @DisplayName("蹂댁긽 ?붿껌 ?묐떟怨??섎졊 ?댁뿭 議고쉶?먯꽌 reward 援ъ“媛 ?좎??쒕떎")
    void rewardFlowKeepsRewardShape() throws Exception {
        MvcResult requestResult =
                mockMvc.perform(
                                post("/missions/{missionId}/request", mission.getId())
                                        .header("Authorization", "Bearer " + MEMBER_TOKEN))
                        .andExpect(status().isOk())
                        .andReturn();
        Long requestId =
                objectMapper
                        .readTree(requestResult.getResponse().getContentAsString())
                        .path("data")
                        .path("requestId")
                        .asLong();

        String respondBody = objectMapper.writeValueAsString(Map.of("status", "APPROVED"));
        MvcResult respondResult =
                mockMvc.perform(
                                put("/rewards/requests/{requestId}/respond", requestId)
                                        .header("Authorization", "Bearer " + OWNER_TOKEN)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(respondBody))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode respondData =
                objectMapper
                        .readTree(respondResult.getResponse().getContentAsString())
                        .path("data");
        assertRewardNode(
                respondData.path("missionItem").path("reward"),
                rewardTemplate.getId(),
                "data reward");

        MvcResult receivedResult =
                mockMvc.perform(
                                get("/rewards/received")
                                        .header("Authorization", "Bearer " + MEMBER_TOKEN)
                                        .param("size", "20"))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode receivedNode =
                objectMapper
                        .readTree(receivedResult.getResponse().getContentAsString())
                        .path("data")
                        .path("rewards")
                        .get(0);
        assertRewardNode(
                receivedNode.path("missionItem").path("reward"),
                rewardTemplate.getId(),
                "data reward");
    }

    @Test
    @DisplayName("보상 수령 내역 조회는 커서 필드와 reward 구조를 함께 반환한다")
    void receivedRewardCursorFieldsAreReturned() throws Exception {
        MissionRequest approved1 =
                missionRequestRepository.save(
                        MissionRequest.builder()
                                .missionItemId(mission.getId())
                                .requesterId(member.getId())
                                .status(MissionRequestStatus.APPROVED)
                                .resolvedById(owner.getId())
                                .resolvedAt(LocalDateTime.now())
                                .build());
        missionRequestRepository.save(
                MissionRequest.builder()
                        .missionItemId(mission.getId())
                        .requesterId(member.getId())
                        .status(MissionRequestStatus.APPROVED)
                        .resolvedById(owner.getId())
                        .resolvedAt(LocalDateTime.now())
                        .build());

        MvcResult receivedResult =
                mockMvc.perform(
                                get("/rewards/received")
                                        .header("Authorization", "Bearer " + MEMBER_TOKEN)
                                        .param("size", "1")
                                        .param("cursor", String.valueOf(approved1.getId() + 1)))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode receivedData =
                objectMapper
                        .readTree(receivedResult.getResponse().getContentAsString())
                        .path("data");
        assertThat(receivedData.path("rewards").size()).isEqualTo(1);
        assertThat(receivedData.has("hasNext")).isTrue();
        assertThat(receivedData.has("nextCursor")).isTrue();
        assertThat(receivedData.path("rewards").get(0).path("missionItem").has("reward")).isTrue();
    }

    @Test
    @DisplayName("DATA 카테고리 템플릿 조회 시, 활성화된 DATA 템플릿만 반환한다")
    void getRewardTemplates_withDataCategory_returnsOnlyActiveDataTemplates() throws Exception {
        RewardTemplate activeGifticonTemplate =
                createTemplate("gifticon reward", RewardCategory.GIFTICON, 3000, true, true);
        RewardTemplate inactiveDataTemplate =
                createTemplate("inactive data reward", RewardCategory.DATA, 2000, true, false);
        RewardTemplate deletedDataTemplate =
                createTemplate("deleted data reward", RewardCategory.DATA, 1000, false, true);
        deletedDataTemplate.delete();

        MvcResult result =
                mockMvc.perform(
                                get("/rewards/templates")
                                        .header("Authorization", "Bearer " + MEMBER_TOKEN)
                                        .param("category", RewardCategory.DATA.name()))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode templates =
                objectMapper.readTree(result.getResponse().getContentAsString()).path("data");

        assertThat(templates.isArray()).isTrue();
        assertThat(templates.size()).isEqualTo(1);
        assertThat(templates.get(0).path("id").asLong()).isEqualTo(rewardTemplate.getId());
        assertThat(templates.get(0).path("name").asText()).isEqualTo("data reward");
        assertThat(templates.get(0).path("category").asText())
                .isEqualTo(RewardCategory.DATA.name());
        assertThat(templates.toString()).doesNotContain(activeGifticonTemplate.getName());
        assertThat(templates.toString()).doesNotContain(inactiveDataTemplate.getName());
        assertThat(templates.toString()).doesNotContain(deletedDataTemplate.getName());
    }

    @Test
    @DisplayName("GIFTICON 카테고리 템플릿 조회 시, 활성화된 GIFTICON 템플릿만 반환한다")
    void getRewardTemplates_withGifticonCategory_returnsOnlyActiveGifticonTemplates()
            throws Exception {
        RewardTemplate activeGifticonTemplate =
                createTemplate("gifticon reward", RewardCategory.GIFTICON, 3000, true, true);
        RewardTemplate inactiveGifticonTemplate =
                createTemplate(
                        "inactive gifticon reward", RewardCategory.GIFTICON, 2500, true, false);
        RewardTemplate deletedGifticonTemplate =
                createTemplate(
                        "deleted gifticon reward", RewardCategory.GIFTICON, 1500, false, true);
        deletedGifticonTemplate.delete();

        MvcResult result =
                mockMvc.perform(
                                get("/rewards/templates")
                                        .header("Authorization", "Bearer " + MEMBER_TOKEN)
                                        .param("category", RewardCategory.GIFTICON.name()))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode templates =
                objectMapper.readTree(result.getResponse().getContentAsString()).path("data");

        assertThat(templates.isArray()).isTrue();
        assertThat(templates.size()).isEqualTo(1);
        assertThat(templates.get(0).path("name").asText())
                .isEqualTo(activeGifticonTemplate.getName());
        assertThat(templates.get(0).path("category").asText())
                .isEqualTo(RewardCategory.GIFTICON.name());
        assertThat(templates.toString()).doesNotContain(rewardTemplate.getName());
        assertThat(templates.toString()).doesNotContain(inactiveGifticonTemplate.getName());
        assertThat(templates.toString()).doesNotContain(deletedGifticonTemplate.getName());
    }

    private RewardTemplate createTemplate(
            String name, RewardCategory category, int price, boolean isSystem, boolean isActive) {
        return rewardTemplateRepository.save(
                RewardTemplate.builder()
                        .name(name)
                        .category(category)
                        .price(price)
                        .isSystem(isSystem)
                        .isActive(isActive)
                        .build());
    }

    private void assertRewardNode(JsonNode rewardNode, long templateId, String expectedName) {
        assertThat(rewardNode.has("rewardId")).isTrue();
        assertThat(rewardNode.path("templateId").asLong()).isEqualTo(templateId);
        assertThat(rewardNode.path("name").asText()).isEqualTo(expectedName);
        assertThat(rewardNode.has("rewardValue")).isFalse();
    }
}
