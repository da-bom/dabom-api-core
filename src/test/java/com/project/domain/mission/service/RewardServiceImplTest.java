package com.project.domain.mission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.domain.customer.enums.RoleType;
import com.project.domain.customer.repository.CustomerRepository;
import com.project.domain.mission.dto.request.RespondRewardRequest;
import com.project.domain.mission.entity.MissionItem;
import com.project.domain.mission.entity.MissionRequest;
import com.project.domain.mission.entity.RewardTemplate;
import com.project.domain.mission.enums.MissionRequestStatus;
import com.project.domain.mission.enums.MissionStatus;
import com.project.domain.mission.enums.RewardCategory;
import com.project.domain.mission.exception.MissionException;
import com.project.domain.mission.model.AuthContext;
import com.project.domain.mission.repository.MissionItemRepository;
import com.project.domain.mission.repository.MissionLogRepository;
import com.project.domain.mission.repository.MissionRequestRepository;
import com.project.domain.mission.repository.RewardTemplateRepository;
import com.project.global.exception.code.MissionErrorCode;

@ExtendWith(MockitoExtension.class)
class RewardServiceImplTest {

    @Mock private MissionRequestRepository missionRequestRepository;
    @Mock private MissionItemRepository missionItemRepository;
    @Mock private MissionLogRepository missionLogRepository;
    @Mock private RewardTemplateRepository rewardTemplateRepository;
    @Mock private CustomerRepository customerRepository;

    @InjectMocks private RewardServiceImpl rewardService;

    @Test
    @DisplayName("OWNER 승인 시 요청은 APPROVED, 미션은 COMPLETED로 전이된다")
    void respondRewardRequest_whenApproved_thenCompleteMission() {
        AuthContext auth = new AuthContext(1L, 10L, RoleType.OWNER);
        MissionRequest request =
                MissionRequest.builder()
                        .id(100L)
                        .missionItemId(200L)
                        .requesterId(2L)
                        .status(MissionRequestStatus.PENDING)
                        .build();
        MissionItem mission =
                MissionItem.builder()
                        .id(200L)
                        .familyId(10L)
                        .targetCustomerId(2L)
                        .createdById(1L)
                        .rewardTemplateId(500L)
                        .missionText("clean room")
                        .rewardValue(100L)
                        .status(MissionStatus.ACTIVE)
                        .build();
        RewardTemplate template =
                RewardTemplate.builder()
                        .id(500L)
                        .name("data")
                        .category(RewardCategory.DATA)
                        .defaultValue(100L)
                        .unit("MB")
                        .isSystem(true)
                        .build();
        var responder = mock(com.project.domain.customer.entity.Customer.class);
        given(responder.getName()).willReturn("owner");
        given(missionRequestRepository.findById(100L)).willReturn(Optional.of(request));
        given(missionItemRepository.findByIdAndFamilyId(200L, 10L)).willReturn(Optional.of(mission));
        given(rewardTemplateRepository.findById(500L)).willReturn(Optional.of(template));
        given(customerRepository.findById(1L)).willReturn(Optional.of(responder));

        var result =
                rewardService.respondRewardRequest(
                        auth, 100L, new RespondRewardRequest("APPROVED", null));

        assertThat(result.status()).isEqualTo("APPROVED");
        assertThat(result.missionItem().status()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("거절 시 요청은 REJECTED로 전이되고 거절 사유가 저장된다")
    void respondRewardRequest_whenRejected_thenRejectReasonSaved() {
        AuthContext auth = new AuthContext(1L, 10L, RoleType.OWNER);
        MissionRequest request =
                MissionRequest.builder()
                        .id(100L)
                        .missionItemId(200L)
                        .requesterId(2L)
                        .status(MissionRequestStatus.PENDING)
                        .build();
        MissionItem mission =
                MissionItem.builder()
                        .id(200L)
                        .familyId(10L)
                        .targetCustomerId(2L)
                        .createdById(1L)
                        .rewardTemplateId(500L)
                        .missionText("clean room")
                        .rewardValue(100L)
                        .status(MissionStatus.ACTIVE)
                        .build();
        RewardTemplate template =
                RewardTemplate.builder()
                        .id(500L)
                        .name("data")
                        .category(RewardCategory.DATA)
                        .defaultValue(100L)
                        .unit("MB")
                        .isSystem(true)
                        .build();
        var responder = mock(com.project.domain.customer.entity.Customer.class);
        given(responder.getName()).willReturn("owner");
        given(missionRequestRepository.findById(100L)).willReturn(Optional.of(request));
        given(missionItemRepository.findByIdAndFamilyId(200L, 10L)).willReturn(Optional.of(mission));
        given(rewardTemplateRepository.findById(500L)).willReturn(Optional.of(template));
        given(customerRepository.findById(1L)).willReturn(Optional.of(responder));

        var result =
                rewardService.respondRewardRequest(
                        auth, 100L, new RespondRewardRequest("REJECTED", "not enough evidence"));

        assertThat(result.status()).isEqualTo("REJECTED");
        assertThat(result.rejectReason()).isEqualTo("not enough evidence");
    }

    @Test
    @DisplayName("OWNER가 아니면 보상 응답은 MISSION_OWNER_ONLY를 반환한다")
    void respondRewardRequest_whenMember_thenThrows() {
        AuthContext auth = new AuthContext(2L, 10L, RoleType.MEMBER);

        assertThatThrownBy(
                        () ->
                                rewardService.respondRewardRequest(
                                        auth, 100L, new RespondRewardRequest("APPROVED", null)))
                .isInstanceOf(MissionException.class)
                .satisfies(
                        e ->
                                assertThat(((MissionException) e).getCode())
                                        .isEqualTo(MissionErrorCode.MISSION_OWNER_ONLY));
    }
}
