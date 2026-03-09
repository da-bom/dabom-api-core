package com.project.domain.mission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.domain.customer.entity.Customer;
import com.project.domain.customer.enums.RoleType;
import com.project.domain.customer.repository.CustomerRepository;
import com.project.domain.mission.dto.request.RespondRewardRequest;
import com.project.domain.mission.entity.MissionItem;
import com.project.domain.mission.entity.MissionRequest;
import com.project.domain.mission.entity.Reward;
import com.project.domain.mission.entity.RewardTemplate;
import com.project.domain.mission.enums.MissionRequestStatus;
import com.project.domain.mission.enums.MissionStatus;
import com.project.domain.mission.enums.RewardCategory;
import com.project.domain.mission.model.AuthContext;
import com.project.domain.mission.repository.MissionItemRepository;
import com.project.domain.mission.repository.MissionLogRepository;
import com.project.domain.mission.repository.MissionRequestRepository;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.MissionErrorCode;

@ExtendWith(MockitoExtension.class)
class RewardServiceImplTest {

    @Mock private MissionRequestRepository missionRequestRepository;
    @Mock private MissionItemRepository missionItemRepository;
    @Mock private MissionLogRepository missionLogRepository;
    @Mock private CustomerRepository customerRepository;

    private RewardServiceImpl rewardService;

    @BeforeEach
    void setUp() {
        rewardService =
                new RewardServiceImpl(
                        missionRequestRepository,
                        missionItemRepository,
                        missionLogRepository,
                        customerRepository);
    }

    @Test
    @DisplayName("OWNER 승인 시 요청은 APPROVED, 미션은 COMPLETED가 된다")
    void respondRewardRequest_whenApproved_thenCompleteMission() {
        AuthContext auth = new AuthContext(1L, 10L, RoleType.OWNER);
        MissionRequest request =
                MissionRequest.builder()
                        .id(100L)
                        .missionItemId(200L)
                        .requesterId(2L)
                        .status(MissionRequestStatus.PENDING)
                        .build();
        MissionItem mission = mission(200L, 10L, reward(900L, 500L, 100L));
        given(missionRequestRepository.findByIdForUpdate(100L)).willReturn(Optional.of(request));
        given(missionItemRepository.findByIdAndFamilyIdForUpdate(200L, 10L))
                .willReturn(Optional.of(mission));
        Customer owner = customer(1L, "owner");
        given(customerRepository.findById(1L)).willReturn(Optional.of(owner));

        var result =
                rewardService.respondRewardRequest(
                        auth, 100L, new RespondRewardRequest("APPROVED", null));

        assertThat(result.status()).isEqualTo("APPROVED");
        assertThat(result.missionItem().status()).isEqualTo("COMPLETED");
        assertThat(result.missionItem().reward().rewardId()).isEqualTo(900L);
    }

    @Test
    @DisplayName("거절 시 rejectReason이 저장된다")
    void respondRewardRequest_whenRejected_thenRejectReasonSaved() {
        AuthContext auth = new AuthContext(1L, 10L, RoleType.OWNER);
        MissionRequest request =
                MissionRequest.builder()
                        .id(100L)
                        .missionItemId(200L)
                        .requesterId(2L)
                        .status(MissionRequestStatus.PENDING)
                        .build();
        MissionItem mission = mission(200L, 10L, reward(900L, 500L, 100L));
        given(missionRequestRepository.findByIdForUpdate(100L)).willReturn(Optional.of(request));
        given(missionItemRepository.findByIdAndFamilyIdForUpdate(200L, 10L))
                .willReturn(Optional.of(mission));
        Customer owner = customer(1L, "owner");
        given(customerRepository.findById(1L)).willReturn(Optional.of(owner));

        var result =
                rewardService.respondRewardRequest(
                        auth, 100L, new RespondRewardRequest("REJECTED", "not enough evidence"));

        assertThat(result.status()).isEqualTo("REJECTED");
        assertThat(result.rejectReason()).isEqualTo("not enough evidence");
        assertThat(result.missionItem().reward().templateId()).isEqualTo(500L);
    }

    @Test
    @DisplayName("수령 보상 목록은 reward 객체를 포함한다")
    void listReceivedRewards_returnsRewardObject() {
        AuthContext auth = new AuthContext(2L, 10L, RoleType.MEMBER);
        MissionRequest approvedRequest =
                MissionRequest.builder()
                        .id(100L)
                        .missionItemId(200L)
                        .requesterId(2L)
                        .status(MissionRequestStatus.APPROVED)
                        .resolvedById(1L)
                        .resolvedAt(java.time.LocalDateTime.now())
                        .build();
        given(
                        missionRequestRepository
                                .findApprovedByTargetCustomerIdOrderByResolvedAtDesc(
                                        org.mockito.ArgumentMatchers.eq(2L),
                                        org.mockito.ArgumentMatchers.isNull(),
                                        org.mockito.ArgumentMatchers.any()))
                .willReturn(List.of(approvedRequest));
        given(missionItemRepository.findAllWithRewardByIdIn(anyIterable()))
                .willReturn(List.of(mission(200L, 10L, reward(900L, 500L, 100L))));
        Customer owner = customer(1L, "owner");
        given(customerRepository.findAllById(anyIterable())).willReturn(List.of(owner));

        var result = rewardService.listReceivedRewards(auth, null, 20);

        assertThat(result.rewards()).hasSize(1);
        assertThat(result.rewards().getFirst().missionItem().reward().rewardId()).isEqualTo(900L);
    }

    @Test
    @DisplayName("OWNER가 아니면 보상 응답은 MISSION_OWNER_ONLY를 반환한다")
    void respondRewardRequest_whenMember_thenThrows() {
        AuthContext auth = new AuthContext(2L, 10L, RoleType.MEMBER);
        RespondRewardRequest request = new RespondRewardRequest("APPROVED", null);

        assertThatThrownBy(() -> rewardService.respondRewardRequest(auth, 100L, request))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(MissionErrorCode.MISSION_OWNER_ONLY));
    }

    private MissionItem mission(Long missionId, Long familyId, Reward reward) {
        return MissionItem.builder()
                .id(missionId)
                .familyId(familyId)
                .targetCustomerId(2L)
                .createdById(1L)
                .reward(reward)
                .missionText("clean room")
                .status(MissionStatus.ACTIVE)
                .build();
    }

    private Reward reward(Long rewardId, Long templateId, Long value) {
        RewardTemplate template =
                RewardTemplate.builder()
                        .id(templateId)
                        .name("data")
                        .category(RewardCategory.DATA)
                        .defaultValue(100L)
                        .unit("MB")
                        .isSystem(true)
                        .build();
        return Reward.builder()
                .id(rewardId)
                .rewardTemplate(template)
                .name("data")
                .category(RewardCategory.DATA)
                .value(value)
                .unit("MB")
                .build();
    }

    private Customer customer(Long id, String name) {
        Customer customer = mock(Customer.class);
        given(customer.getId()).willReturn(id);
        given(customer.getName()).willReturn(name);
        return customer;
    }
}
