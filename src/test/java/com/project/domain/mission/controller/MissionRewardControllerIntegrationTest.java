package com.project.domain.mission.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import com.project.domain.mission.entity.MissionLog;
import com.project.domain.mission.entity.MissionRequest;
import com.project.domain.mission.entity.RewardTemplate;
import com.project.domain.mission.enums.MissionLogActionType;
import com.project.domain.mission.enums.MissionRequestStatus;
import com.project.domain.mission.enums.MissionStatus;
import com.project.domain.mission.enums.RewardCategory;
import com.project.domain.mission.repository.MissionItemRepository;
import com.project.domain.mission.repository.MissionLogRepository;
import com.project.domain.mission.repository.MissionRequestRepository;
import com.project.domain.mission.repository.RewardTemplateRepository;
import com.project.global.auth.JwtTokenUtil;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@EmbeddedKafka(
        partitions = 1,
        topics = {"usage-events"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
class MissionRewardControllerIntegrationTest {

    private static final String OWNER_TOKEN = "OWNER_TOKEN";
    private static final String MEMBER_TOKEN = "MEMBER_TOKEN";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private CustomerRepository customerRepository;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private FamilyMemberRepository familyMemberRepository;
    @Autowired private MissionItemRepository missionItemRepository;
    @Autowired private MissionRequestRepository missionRequestRepository;
    @Autowired private MissionLogRepository missionLogRepository;
    @Autowired private RewardTemplateRepository rewardTemplateRepository;

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

        mission =
                missionItemRepository.save(
                        MissionItem.builder()
                                .familyId(family.getId())
                                .targetCustomerId(member.getId())
                                .createdById(owner.getId())
                                .rewardTemplateId(rewardTemplate.getId())
                                .missionText("clean room")
                                .rewardValue(200L)
                                .status(MissionStatus.ACTIVE)
                                .build());

        lenient().when(jwtTokenUtil.verify(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);
        lenient().when(jwtTokenUtil.getMemberId(OWNER_TOKEN)).thenReturn(owner.getId());
        lenient().when(jwtTokenUtil.getMemberId(MEMBER_TOKEN)).thenReturn(member.getId());
        lenient().when(jwtTokenUtil.getRole(OWNER_TOKEN)).thenReturn(RoleType.OWNER);
        lenient().when(jwtTokenUtil.getRole(MEMBER_TOKEN)).thenReturn(RoleType.MEMBER);
    }

    @Test
    @DisplayName("미션 생성/요청/응답/수령 조회 플로우가 정상 동작한다")
    void missionRewardFlowWorks() throws Exception {
        String createBody =
                objectMapper.writeValueAsString(
                        Map.of(
                                "targetCustomerId", member.getId(),
                                "rewardTemplateId", rewardTemplate.getId(),
                                "missionText", "wash dishes",
                                "rewardCategory", "DATA",
                                "rewardValue", 300));

        MvcResult createResult =
                mockMvc.perform(
                                post("/missions")
                                        .header("Authorization", "Bearer " + OWNER_TOKEN)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(createBody))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode createData =
                objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data");
        Long createdMissionId = createData.path("missionItemId").asLong();
        assertThat(createdMissionId).isPositive();

        MvcResult requestResult =
                mockMvc.perform(
                                post("/missions/{missionId}/request", mission.getId())
                                        .header("Authorization", "Bearer " + MEMBER_TOKEN))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode requestData =
                objectMapper.readTree(requestResult.getResponse().getContentAsString()).path("data");
        Long requestId = requestData.path("requestId").asLong();
        assertThat(requestId).isPositive();

        String respondBody = objectMapper.writeValueAsString(Map.of("status", "APPROVED"));
        mockMvc.perform(
                        put("/rewards/requests/{requestId}/respond", requestId)
                                .header("Authorization", "Bearer " + OWNER_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(respondBody))
                .andExpect(status().isOk());

        MvcResult receivedResult =
                mockMvc.perform(
                                get("/rewards/received")
                                        .header("Authorization", "Bearer " + MEMBER_TOKEN)
                                        .param("size", "20"))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode receivedData =
                objectMapper.readTree(receivedResult.getResponse().getContentAsString()).path("data");
        assertThat(receivedData.path("content").isArray()).isTrue();
        assertThat(receivedData.path("content").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("OwnerOnly 엔드포인트는 MEMBER 접근 시 403을 반환한다")
    void ownerOnlyEndpointForbiddenToMember() throws Exception {
        String createBody =
                objectMapper.writeValueAsString(
                        Map.of(
                                "targetCustomerId", member.getId(),
                                "rewardTemplateId", rewardTemplate.getId(),
                                "missionText", "wash dishes",
                                "rewardCategory", "DATA",
                                "rewardValue", 300));

        mockMvc.perform(
                        post("/missions")
                                .header("Authorization", "Bearer " + MEMBER_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("로그/수령내역은 커서 페이지네이션 필드(content,nextCursor,hasNext)를 반환한다")
    void cursorPaginationFieldsAreReturned() throws Exception {
        missionItemRepository.save(
                MissionItem.builder()
                        .familyId(family.getId())
                        .targetCustomerId(member.getId())
                        .createdById(owner.getId())
                        .rewardTemplateId(rewardTemplate.getId())
                        .missionText("another mission")
                        .rewardValue(250L)
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
        assertThat(missionData.path("content").size()).isEqualTo(1);
        assertThat(missionData.path("hasNext").asBoolean()).isTrue();

        MissionLog log1 =
                MissionLog.builder()
                        .missionItemId(mission.getId())
                        .actorId(owner.getId())
                        .actionType(MissionLogActionType.CREATED)
                        .message("created")
                        .build();
        MissionLog log2 =
                MissionLog.builder()
                        .missionItemId(mission.getId())
                        .actorId(member.getId())
                        .actionType(MissionLogActionType.REQUESTED)
                        .message("requested")
                        .build();
        MissionLog log3 =
                MissionLog.builder()
                        .missionItemId(mission.getId())
                        .actorId(owner.getId())
                        .actionType(MissionLogActionType.APPROVED)
                        .message("approved")
                        .build();
        missionLogRepository.save(log1);
        missionLogRepository.save(log2);
        missionLogRepository.save(log3);

        MissionRequest approved1 =
                missionRequestRepository.save(
                        MissionRequest.builder()
                                .missionItemId(mission.getId())
                                .requesterId(member.getId())
                                .status(MissionRequestStatus.APPROVED)
                                .resolvedById(owner.getId())
                                .resolvedAt(java.time.LocalDateTime.now())
                                .build());
        missionRequestRepository.save(
                MissionRequest.builder()
                        .missionItemId(mission.getId())
                        .requesterId(member.getId())
                        .status(MissionRequestStatus.APPROVED)
                        .resolvedById(owner.getId())
                        .resolvedAt(java.time.LocalDateTime.now())
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
        assertThat(logsData.path("content").size()).isEqualTo(2);
        assertThat(logsData.path("hasNext").asBoolean()).isFalse();

        MvcResult receivedResult =
                mockMvc.perform(
                                get("/rewards/received")
                                        .header("Authorization", "Bearer " + MEMBER_TOKEN)
                                        .param("size", "1")
                                        .param("cursor", String.valueOf(approved1.getId() + 1)))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode receivedData =
                objectMapper.readTree(receivedResult.getResponse().getContentAsString()).path("data");
        assertThat(receivedData.path("content").size()).isEqualTo(1);
        assertThat(receivedData.has("hasNext")).isTrue();
        assertThat(receivedData.has("nextCursor")).isTrue();

        mockMvc.perform(
                        delete("/missions/{missionId}", mission.getId())
                                .header("Authorization", "Bearer " + OWNER_TOKEN))
                .andExpect(status().isOk());
    }
}
