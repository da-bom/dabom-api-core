package com.project.domain.mission.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

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
import com.project.domain.mission.entity.MissionLog;
import com.project.domain.mission.enums.MissionLogActionType;
import com.project.domain.mission.enums.MissionStatus;
import com.project.domain.mission.repository.MissionItemRepository;
import com.project.domain.mission.repository.MissionLogRepository;
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
class MissionControllerIntegrationTest {

    private static final String OWNER_TOKEN = "OWNER_TOKEN";
    private static final String MEMBER_TOKEN = "MEMBER_TOKEN";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private CustomerRepository customerRepository;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private FamilyMemberRepository familyMemberRepository;
    @Autowired private MissionItemRepository missionItemRepository;
    @Autowired private MissionLogRepository missionLogRepository;
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
                                .defaultValue(200L)
                                .unit("MB")
                                .isSystem(true)
                                .build());

        Reward reward =
                rewardRepository.save(
                        Reward.builder()
                                .rewardTemplate(rewardTemplate)
                                .name("data reward")
                                .category(RewardCategory.DATA)
                                .value(200L)
                                .unit("MB")
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
    @DisplayName("미션 생성과 완료 요청, 목록/로그 조회에서 reward 정보가 유지된다")
    void missionFlowKeepsRewardShape() throws Exception {
        String createBody =
                objectMapper.writeValueAsString(
                        Map.of(
                                "targetCustomerId",
                                member.getId(),
                                "rewardTemplateId",
                                rewardTemplate.getId(),
                                "missionText",
                                "wash dishes",
                                "rewardValue",
                                300,
                                "rewardCategory",
                                "DATA"));

        MvcResult createResult =
                mockMvc.perform(
                                post("/missions")
                                        .header("Authorization", "Bearer " + OWNER_TOKEN)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(createBody))
                        .andExpect(status().isOk())
                        .andReturn();
        Long createdMissionId =
                objectMapper
                        .readTree(createResult.getResponse().getContentAsString())
                        .path("data")
                        .path("missionItemId")
                        .asLong();
        assertThat(createdMissionId).isPositive();

        MvcResult requestResult =
                mockMvc.perform(
                                post("/missions/{missionId}/request", mission.getId())
                                        .header("Authorization", "Bearer " + MEMBER_TOKEN))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode requestData =
                objectMapper
                        .readTree(requestResult.getResponse().getContentAsString())
                        .path("data");
        assertRewardNode(
                requestData.path("missionItem").path("reward"),
                rewardTemplate.getId(),
                "data reward",
                200L);

        MvcResult missionListResult =
                mockMvc.perform(
                                get("/missions")
                                        .header("Authorization", "Bearer " + MEMBER_TOKEN)
                                        .param("size", "20"))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode missionList =
                objectMapper
                        .readTree(missionListResult.getResponse().getContentAsString())
                        .path("data")
                        .path("missions");
        JsonNode existingMissionNode = null;
        for (JsonNode node : missionList) {
            if (node.path("missionItemId").asLong() == mission.getId()) {
                existingMissionNode = node;
                break;
            }
        }
        assertThat(existingMissionNode).isNotNull();

        JsonNode nonNullExistingMissionNode = Objects.requireNonNull(existingMissionNode);
        assertRewardNode(
                nonNullExistingMissionNode.path("reward"),
                rewardTemplate.getId(),
                "data reward",
                200L);

        MvcResult logsResult =
                mockMvc.perform(
                                get("/missions/logs")
                                        .header("Authorization", "Bearer " + OWNER_TOKEN)
                                        .param("size", "20"))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode logNode =
                objectMapper
                        .readTree(logsResult.getResponse().getContentAsString())
                        .path("data")
                        .path("missions")
                        .get(0);
        assertRewardNode(
                logNode.path("missionItem").path("reward"),
                rewardTemplate.getId(),
                "data reward",
                200L);
    }

    @Test
    @DisplayName("POST /missions 요청의 rewardCategory 필드는 무시된다")
    void createMission_ignoresRewardCategoryField() throws Exception {
        String createBody =
                objectMapper.writeValueAsString(
                        Map.of(
                                "targetCustomerId",
                                member.getId(),
                                "rewardTemplateId",
                                rewardTemplate.getId(),
                                "missionText",
                                "wash dishes",
                                "rewardValue",
                                300,
                                "rewardCategory",
                                "MONEY"));

        MvcResult result =
                mockMvc.perform(
                                post("/missions")
                                        .header("Authorization", "Bearer " + OWNER_TOKEN)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(createBody))
                        .andExpect(status().isOk())
                        .andReturn();

        Long createdMissionId =
                objectMapper
                        .readTree(result.getResponse().getContentAsString())
                        .path("data")
                        .path("missionItemId")
                        .asLong();
        MissionItem createdMission = missionItemRepository.findById(createdMissionId).orElseThrow();
        assertThat(createdMission.getReward().getCategory()).isEqualTo(RewardCategory.DATA);
    }

    @Test
    @DisplayName("OwnerOnly 엔드포인트는 MEMBER 요청에 403을 반환한다")
    void ownerOnlyEndpointForbiddenToMember() throws Exception {
        String createBody =
                objectMapper.writeValueAsString(
                        Map.of(
                                "targetCustomerId",
                                member.getId(),
                                "rewardTemplateId",
                                rewardTemplate.getId(),
                                "missionText",
                                "wash dishes",
                                "rewardValue",
                                300));

        mockMvc.perform(
                        post("/missions")
                                .header("Authorization", "Bearer " + MEMBER_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("미션 목록과 로그 조회는 커서 필드와 reward 구조를 함께 반환한다")
    void cursorPaginationFieldsAreReturned() throws Exception {
        Reward reward =
                rewardRepository.save(
                        Reward.builder()
                                .rewardTemplate(rewardTemplate)
                                .name("data reward")
                                .category(RewardCategory.DATA)
                                .value(250L)
                                .unit("MB")
                                .build());
        missionItemRepository.save(
                MissionItem.builder()
                        .familyId(family.getId())
                        .targetCustomerId(member.getId())
                        .createdById(owner.getId())
                        .reward(reward)
                        .missionText("another mission")
                        .status(MissionStatus.ACTIVE)
                        .build());

        MvcResult missionListResult =
                mockMvc.perform(
                                get("/missions")
                                        .header("Authorization", "Bearer " + OWNER_TOKEN)
                                        .param("size", "1"))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode missionData =
                objectMapper
                        .readTree(missionListResult.getResponse().getContentAsString())
                        .path("data");
        assertThat(missionData.path("missions").size()).isEqualTo(1);
        assertThat(missionData.path("hasNext").asBoolean()).isTrue();
        assertThat(missionData.path("missions").get(0).has("reward")).isTrue();

        missionLogRepository.save(
                MissionLog.builder()
                        .missionItemId(mission.getId())
                        .actorId(owner.getId())
                        .actionType(MissionLogActionType.CREATED)
                        .message("created")
                        .build());
        missionLogRepository.save(
                MissionLog.builder()
                        .missionItemId(mission.getId())
                        .actorId(member.getId())
                        .actionType(MissionLogActionType.REQUESTED)
                        .message("requested")
                        .build());
        missionLogRepository.save(
                MissionLog.builder()
                        .missionItemId(mission.getId())
                        .actorId(owner.getId())
                        .actionType(MissionLogActionType.APPROVED)
                        .message("approved")
                        .build());

        MvcResult logsResult =
                mockMvc.perform(
                                get("/missions/logs")
                                        .header("Authorization", "Bearer " + OWNER_TOKEN)
                                        .param("size", "2"))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode logsData =
                objectMapper.readTree(logsResult.getResponse().getContentAsString()).path("data");
        assertThat(logsData.path("missions").size()).isEqualTo(2);
        assertThat(logsData.path("hasNext").asBoolean()).isTrue();
        assertThat(logsData.path("missions").get(0).path("missionItem").has("reward")).isTrue();

        mockMvc.perform(
                        delete("/missions/{missionId}", mission.getId())
                                .header("Authorization", "Bearer " + OWNER_TOKEN))
                .andExpect(status().isOk());
    }

    private void assertRewardNode(
            JsonNode rewardNode, long templateId, String expectedName, long expectedValue) {
        assertThat(rewardNode.has("rewardId")).isTrue();
        assertThat(rewardNode.path("templateId").asLong()).isEqualTo(templateId);
        assertThat(rewardNode.path("name").asText()).isEqualTo(expectedName);
        assertThat(rewardNode.path("value").asLong()).isEqualTo(expectedValue);
    }
}
