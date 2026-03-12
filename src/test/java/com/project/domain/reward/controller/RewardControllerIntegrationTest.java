package com.project.domain.reward.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
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
    @MockitoBean private KafkaTemplate<String, Object> kafkaTemplate;

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

        lenient()
                .when(jwtTokenUtil.verify(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(null);
        lenient().when(jwtTokenUtil.getMemberId(OWNER_TOKEN)).thenReturn(owner.getId());
        lenient().when(jwtTokenUtil.getMemberId(MEMBER_TOKEN)).thenReturn(member.getId());
        lenient().when(jwtTokenUtil.getRole(OWNER_TOKEN)).thenReturn(RoleType.OWNER);
        lenient().when(jwtTokenUtil.getRole(MEMBER_TOKEN)).thenReturn(RoleType.MEMBER);
    }

    @Test
    @DisplayName("보상 요청 응답과 수령 내역 조회에서 reward 구조가 유지된다")
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

    private void assertRewardNode(JsonNode rewardNode, long templateId, String expectedName) {
        assertThat(rewardNode.has("rewardId")).isTrue();
        assertThat(rewardNode.path("templateId").asLong()).isEqualTo(templateId);
        assertThat(rewardNode.path("name").asText()).isEqualTo(expectedName);
        assertThat(rewardNode.has("rewardValue")).isFalse();
    }
}
