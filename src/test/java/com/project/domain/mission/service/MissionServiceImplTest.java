package com.project.domain.mission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.project.domain.customer.entity.Customer;
import com.project.domain.customer.enums.RoleType;
import com.project.domain.customer.repository.CustomerRepository;
import com.project.domain.family.entity.FamilyMember;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.mission.dto.request.CreateMissionRequest;
import com.project.domain.mission.entity.MissionItem;
import com.project.domain.mission.entity.MissionLog;
import com.project.domain.mission.entity.MissionRequest;
import com.project.domain.mission.enums.MissionLogActionType;
import com.project.domain.mission.enums.MissionRequestStatus;
import com.project.domain.mission.enums.MissionStatus;
import com.project.domain.mission.model.MissionListResult;
import com.project.domain.mission.repository.MissionItemRepository;
import com.project.domain.mission.repository.MissionLogRepository;
import com.project.domain.mission.repository.MissionRequestRepository;
import com.project.domain.reward.entity.Reward;
import com.project.domain.reward.entity.RewardTemplate;
import com.project.domain.reward.enums.RewardCategory;
import com.project.domain.reward.service.RewardSnapshotService;
import com.project.global.auth.model.AuthContext;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.MissionErrorCode;

@ExtendWith(MockitoExtension.class)
class MissionServiceImplTest {

    @Mock private MissionItemRepository missionItemRepository;
    @Mock private MissionRequestRepository missionRequestRepository;
    @Mock private MissionLogRepository missionLogRepository;
    @Mock private RewardSnapshotService rewardSnapshotService;
    @Mock private CustomerRepository customerRepository;
    @Mock private FamilyMemberRepository familyMemberRepository;

    @InjectMocks private MissionServiceImpl missionService;

    @Test
    @DisplayName("OWNER는 reward 스냅샷이 포함된 미션 목록을 조회한다")
    void listMissions_ownerReadsFamilyScope() {
        AuthContext auth = new AuthContext(1L, 10L, RoleType.OWNER);
        MissionItem mission = mission(100L, 10L, 2L, 1L, reward(900L, 500L), "clean room");

        given(
                        missionItemRepository.findByFamilyScope(
                                10L, MissionStatus.ACTIVE, null, PageRequest.of(0, 21)))
                .willReturn(List.of(mission));
        given(
                        missionRequestRepository.findByMissionItemIdInOrderByCreatedAtDescIdDesc(
                                java.util.Set.of(100L)))
                .willReturn(
                        List.of(
                                MissionRequest.builder()
                                        .id(200L)
                                        .missionItemId(100L)
                                        .requesterId(2L)
                                        .status(MissionRequestStatus.PENDING)
                                        .build()));
        Customer owner = customer(1L, "owner");
        Customer member = customer(2L, "member");
        given(customerRepository.findAllById(anyIterable())).willReturn(List.of(owner, member));

        var result = missionService.listMissions(auth, null, 20);

        assertThat(result.missions()).hasSize(1);
        assertThat(result.missions().getFirst().requestStatus()).isEqualTo("PENDING");
        assertThat(result.missions().getFirst().reward().rewardId()).isEqualTo(900L);
        assertThat(result.missions().getFirst().reward().templateId()).isEqualTo(500L);
        verify(missionItemRepository)
                .findByFamilyScope(10L, MissionStatus.ACTIVE, null, PageRequest.of(0, 21));
    }

    @Test
    @DisplayName("미션 목록 조회 시 요청 상태가 APPROVED 인 미션은 제외하고 PENDING 또는 요청 이력 없는 미션만 반환한다")
    void listMissions_returnsOnlyPendingOrNoRequestStatus() {
        AuthContext auth = new AuthContext(1L, 10L, RoleType.OWNER);
        MissionItem pendingMission =
                mission(100L, 10L, 2L, 1L, reward(900L, 500L), "pending mission");
        MissionItem approvedMission =
                mission(99L, 10L, 2L, 1L, reward(901L, 500L), "approved mission");
        MissionItem noRequestMission =
                mission(98L, 10L, 2L, 1L, reward(902L, 500L), "no request mission");

        given(
                        missionItemRepository.findByFamilyScope(
                                10L, MissionStatus.ACTIVE, null, PageRequest.of(0, 21)))
                .willReturn(List.of(pendingMission, approvedMission, noRequestMission));
        given(
                        missionRequestRepository.findByMissionItemIdInOrderByCreatedAtDescIdDesc(
                                java.util.Set.of(100L, 99L, 98L)))
                .willReturn(
                        List.of(
                                MissionRequest.builder()
                                        .id(201L)
                                        .missionItemId(100L)
                                        .requesterId(2L)
                                        .status(MissionRequestStatus.PENDING)
                                        .build(),
                                MissionRequest.builder()
                                        .id(202L)
                                        .missionItemId(99L)
                                        .requesterId(2L)
                                        .status(MissionRequestStatus.APPROVED)
                                        .build()));
        Customer owner = customer(1L, "owner");
        Customer member = customer(2L, "member");
        given(customerRepository.findAllById(anyIterable())).willReturn(List.of(owner, member));

        var result = missionService.listMissions(auth, null, 20);

        assertThat(result.missions()).hasSize(2);
        assertThat(
                        result.missions().stream()
                                .map(MissionListResult.MissionCard::missionItemId)
                                .toList())
                .containsExactly(100L, 98L);
        assertThat(result.missions().get(0).requestStatus()).isEqualTo("PENDING");
        assertThat(result.missions().get(1).requestStatus()).isNull();
    }

    @Test
    @DisplayName("미션 로그 응답은 reward 객체를 포함한다")
    void listMissionLogs_readsFromMissionLogRepository() {
        AuthContext auth = new AuthContext(1L, 10L, RoleType.OWNER);
        MissionLog log =
                MissionLog.builder()
                        .id(300L)
                        .missionItemId(100L)
                        .actorId(1L)
                        .actionType(MissionLogActionType.CREATED)
                        .message("Mission created")
                        .build();
        MissionItem mission = mission(100L, 10L, 2L, 1L, reward(900L, 500L), "clean room");

        given(missionLogRepository.findByFamilyScope(10L, null, PageRequest.of(0, 21)))
                .willReturn(List.of(log));
        given(missionItemRepository.findAllWithRewardByIdIn(anyIterable()))
                .willReturn(List.of(mission));
        Customer owner = customer(1L, "owner");
        Customer member = customer(2L, "member");
        given(customerRepository.findAllById(anyIterable())).willReturn(List.of(owner, member));

        var result = missionService.listMissionLogs(auth, null, 20);

        assertThat(result.missions()).hasSize(1);
        assertThat(result.missions().getFirst().logId()).isEqualTo(300L);
        assertThat(result.missions().getFirst().actionType()).isEqualTo("CREATED");
        assertThat(result.missions().getFirst().missionItem().reward().rewardId()).isEqualTo(900L);
        verify(missionLogRepository).findByFamilyScope(10L, null, PageRequest.of(0, 21));
    }

    @Test
    @DisplayName("미션 요청 이력 조회는 MissionRequest.status를 기준으로 응답한다")
    void listMissionRequestHistory_readsMissionRequestHistory() {
        AuthContext auth = new AuthContext(1L, 10L, RoleType.OWNER);
        MissionItem mission = mission(100L, 10L, 2L, 1L, reward(900L, 500L), "clean room");
        MissionRequest request =
                MissionRequest.builder()
                        .id(200L)
                        .missionItemId(100L)
                        .requesterId(2L)
                        .status(MissionRequestStatus.REJECTED)
                        .resolvedById(1L)
                        .rejectReason("photo missing")
                        .build();

        given(missionRequestRepository.findByFamilyIdOrderByIdDesc(10L, null, PageRequest.of(0, 21)))
                .willReturn(List.of(request));
        given(missionItemRepository.findAllWithRewardByIdIn(anyIterable()))
                .willReturn(List.of(mission));
        Customer owner = customer(1L, "owner");
        Customer member = customer(2L, "member");
        given(customerRepository.findAllById(anyIterable())).willReturn(List.of(owner, member));

        var result = missionService.listMissionRequestHistory(auth, null, 20);

        assertThat(result.requests()).hasSize(1);
        assertThat(result.requests().getFirst().requestId()).isEqualTo(200L);
        assertThat(result.requests().getFirst().status()).isEqualTo("REJECTED");
        assertThat(result.requests().getFirst().rejectReason()).isEqualTo("photo missing");
        assertThat(result.requests().getFirst().requestedBy().name()).isEqualTo("member");
        assertThat(result.requests().getFirst().respondedBy().name()).isEqualTo("owner");
    }

    @Test
    @DisplayName("미션 생성 시 Reward 스냅샷을 먼저 저장하고 mission에 연결한다")
    void createMission_createsRewardSnapshot() {
        AuthContext auth = new AuthContext(1L, 10L, RoleType.OWNER);
        CreateMissionRequest request = new CreateMissionRequest("clean room", 2L, 500L);
        RewardTemplate template =
                RewardTemplate.builder()
                        .id(500L)
                        .name("메가커피 아메리카노")
                        .category(RewardCategory.GIFTICON)
                        .price(4500)
                        .isSystem(true)
                        .isActive(true)
                        .build();
        FamilyMember target =
                FamilyMember.builder().familyId(10L).customerId(2L).role(RoleType.MEMBER).build();
        Reward savedReward =
                Reward.builder()
                        .id(900L)
                        .rewardTemplate(template)
                        .name("메가커피 아메리카노")
                        .category(RewardCategory.GIFTICON)
                        .thumbnailUrl("/rewards/mega-coffee.jpg")
                        .build();

        given(familyMemberRepository.findByCustomerId(2L)).willReturn(Optional.of(target));
        given(rewardSnapshotService.createFromTemplate(500L)).willReturn(savedReward);
        given(missionItemRepository.save(any(MissionItem.class)))
                .willAnswer(
                        invocation -> {
                            MissionItem item = invocation.getArgument(0);
                            return MissionItem.builder()
                                    .id(300L)
                                    .familyId(item.getFamilyId())
                                    .targetCustomerId(item.getTargetCustomerId())
                                    .createdById(item.getCreatedById())
                                    .reward(item.getReward())
                                    .missionText(item.getMissionText())
                                    .status(item.getStatus())
                                    .build();
                        });

        var result = missionService.createMission(auth, request);

        assertThat(result.missionItemId()).isEqualTo(300L);
        verify(rewardSnapshotService).createFromTemplate(500L);
        verify(missionItemRepository).save(any(MissionItem.class));
    }

    @Test
    @DisplayName("RewardTemplate이 바뀌어도 기존 요청 응답은 생성된 Reward 스냅샷을 사용한다")
    void requestMissionApproval_usesRewardSnapshot() {
        AuthContext auth = new AuthContext(2L, 10L, RoleType.MEMBER);
        MissionItem mission =
                mission(
                        100L,
                        10L,
                        2L,
                        1L,
                        Reward.builder()
                                .id(900L)
                                .rewardTemplate(
                                        RewardTemplate.builder()
                                                .id(500L)
                                                .name("new template name")
                                                .category(RewardCategory.DATA)
                                                .price(5000)
                                                .isSystem(true)
                                                .isActive(true)
                                                .build())
                                .name("old snapshot name")
                                .category(RewardCategory.GIFTICON)
                                .thumbnailUrl("/rewards/old.jpg")
                                .build(),
                        "clean room");

        given(missionItemRepository.findByIdAndFamilyIdForUpdate(100L, 10L))
                .willReturn(Optional.of(mission));
        given(
                        missionRequestRepository.existsByMissionItemIdAndRequesterIdAndStatus(
                                100L, 2L, MissionRequestStatus.PENDING))
                .willReturn(false);
        given(missionRequestRepository.save(any(MissionRequest.class)))
                .willReturn(
                        MissionRequest.builder()
                                .id(200L)
                                .missionItemId(100L)
                                .requesterId(2L)
                                .status(MissionRequestStatus.PENDING)
                                .build());
        Customer member = namedCustomer("member");
        given(customerRepository.findById(2L)).willReturn(Optional.of(member));

        var result = missionService.requestMissionApproval(auth, 100L);

        assertThat(result.missionItem().reward().name()).isEqualTo("old snapshot name");
        assertThat(result.missionItem().reward().category()).isEqualTo(RewardCategory.GIFTICON);
        assertThat(result.missionItem().reward().templateId()).isEqualTo(500L);
    }

    @Test
    @DisplayName("본인에게 할당되지 않은 미션 요청은 MISSION_NOT_ASSIGNED를 반환한다")
    void requestMissionApproval_whenMissionNotAssigned_thenThrows() {
        AuthContext auth = new AuthContext(2L, 10L, RoleType.MEMBER);
        MissionItem mission = mission(100L, 10L, 3L, 1L, reward(900L, 500L), "clean room");
        given(missionItemRepository.findByIdAndFamilyIdForUpdate(100L, 10L))
                .willReturn(Optional.of(mission));

        assertThatThrownBy(() -> missionService.requestMissionApproval(auth, 100L))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(MissionErrorCode.MISSION_NOT_ASSIGNED));
    }

    @Test
    @DisplayName("MEMBER가 미션 생성을 요청하면 MISSION_OWNER_ONLY를 반환한다")
    void createMission_whenMember_thenThrows() {
        AuthContext auth = new AuthContext(2L, 10L, RoleType.MEMBER);
        CreateMissionRequest request = new CreateMissionRequest("clean room", 2L, 500L);

        assertThatThrownBy(() -> missionService.createMission(auth, request))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(MissionErrorCode.MISSION_OWNER_ONLY));
    }

    private MissionItem mission(
            Long missionId,
            Long familyId,
            Long targetCustomerId,
            Long createdById,
            Reward reward,
            String text) {
        return MissionItem.builder()
                .id(missionId)
                .familyId(familyId)
                .targetCustomerId(targetCustomerId)
                .createdById(createdById)
                .reward(reward)
                .missionText(text)
                .status(MissionStatus.ACTIVE)
                .build();
    }

    private Reward reward(Long rewardId, Long templateId) {
        RewardTemplate template =
                RewardTemplate.builder()
                        .id(templateId)
                        .name("data")
                        .category(RewardCategory.DATA)
                        .price(5000)
                        .isSystem(true)
                        .isActive(true)
                        .build();
        return Reward.builder()
                .id(rewardId)
                .rewardTemplate(template)
                .name("data")
                .category(RewardCategory.DATA)
                .thumbnailUrl("/rewards/data.jpg")
                .build();
    }

    private Customer customer(Long id, String name) {
        Customer customer = mock(Customer.class);
        given(customer.getId()).willReturn(id);
        given(customer.getName()).willReturn(name);
        return customer;
    }

    private Customer namedCustomer(String name) {
        Customer customer = mock(Customer.class);
        given(customer.getName()).willReturn(name);
        return customer;
    }
}
