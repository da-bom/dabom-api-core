package com.project.domain.mission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.domain.customer.enums.RoleType;
import com.project.domain.customer.repository.CustomerRepository;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.mission.dto.request.CreateMissionRequest;
import com.project.domain.mission.entity.MissionItem;
import com.project.domain.mission.entity.RewardTemplate;
import com.project.domain.mission.enums.MissionRequestStatus;
import com.project.domain.mission.enums.MissionStatus;
import com.project.domain.mission.enums.RewardCategory;
import com.project.global.exception.ApplicationException;
import com.project.domain.mission.model.AuthContext;
import com.project.domain.mission.repository.MissionItemRepository;
import com.project.domain.mission.repository.MissionLogRepository;
import com.project.domain.mission.repository.MissionRequestRepository;
import com.project.domain.mission.repository.RewardTemplateRepository;
import com.project.global.exception.code.MissionErrorCode;

@ExtendWith(MockitoExtension.class)
class MissionServiceImplTest {

    @Mock private MissionItemRepository missionItemRepository;
    @Mock private MissionRequestRepository missionRequestRepository;
    @Mock private MissionLogRepository missionLogRepository;
    @Mock private RewardTemplateRepository rewardTemplateRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private FamilyMemberRepository familyMemberRepository;

    @InjectMocks private MissionServiceImpl missionService;

    @Test
    @DisplayName("OWNER는 family 기준으로 미션 목록을 조회한다")
    void listMissions_ownerReadsFamilyScope() {
        AuthContext auth = new AuthContext(1L, 10L, RoleType.OWNER);
        MissionItem mission =
                MissionItem.builder()
                        .id(100L)
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
        given(missionItemRepository.findByFamilyScope(10L, null, null, PageRequest.of(0, 21)))
                .willReturn(List.of(mission));
        var owner = mock(com.project.domain.customer.entity.Customer.class);
        given(owner.getId()).willReturn(1L);
        given(owner.getName()).willReturn("owner");
        var member = mock(com.project.domain.customer.entity.Customer.class);
        given(member.getId()).willReturn(2L);
        given(member.getName()).willReturn("member");
        given(customerRepository.findAllById(anyIterable())).willReturn(List.of(owner, member));
        given(rewardTemplateRepository.findAllById(anyIterable())).willReturn(List.of(template));

        var result = missionService.listMissions(auth, null, null, 20);

        assertThat(result.missions()).hasSize(1);
        verify(missionItemRepository).findByFamilyScope(10L, null, null, PageRequest.of(0, 21));
    }

    @Test
    @DisplayName("본인에게 할당되지 않은 미션 요청은 MISSION_NOT_ASSIGNED를 반환한다")
    void requestMissionApproval_whenMissionNotAssigned_thenThrows() {
        AuthContext auth = new AuthContext(2L, 10L, RoleType.MEMBER);
        MissionItem mission =
                MissionItem.builder()
                        .id(100L)
                        .familyId(10L)
                        .targetCustomerId(3L)
                        .createdById(1L)
                        .rewardTemplateId(500L)
                        .missionText("clean room")
                        .rewardValue(100L)
                        .status(MissionStatus.ACTIVE)
                        .build();
        given(missionItemRepository.findByIdAndFamilyId(100L, 10L)).willReturn(Optional.of(mission));

        assertThatThrownBy(() -> missionService.requestMissionApproval(auth, 100L))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(MissionErrorCode.MISSION_NOT_ASSIGNED));
    }

    @Test
    @DisplayName("중복 대기 요청이 있으면 MISSION_REQUEST_DUPLICATED를 반환한다")
    void requestMissionApproval_whenPendingExists_thenThrows() {
        AuthContext auth = new AuthContext(2L, 10L, RoleType.MEMBER);
        MissionItem mission =
                MissionItem.builder()
                        .id(100L)
                        .familyId(10L)
                        .targetCustomerId(2L)
                        .createdById(1L)
                        .rewardTemplateId(500L)
                        .missionText("clean room")
                        .rewardValue(100L)
                        .status(MissionStatus.ACTIVE)
                        .build();
        given(missionItemRepository.findByIdAndFamilyId(100L, 10L)).willReturn(Optional.of(mission));
        given(missionRequestRepository.existsByMissionItemIdAndRequesterIdAndStatus(
                        100L, 2L, MissionRequestStatus.PENDING))
                .willReturn(true);

        assertThatThrownBy(() -> missionService.requestMissionApproval(auth, 100L))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(MissionErrorCode.MISSION_REQUEST_DUPLICATED));
    }

    @Test
    @DisplayName("MEMBER가 미션 생성을 요청하면 MISSION_OWNER_ONLY를 반환한다")
    void createMission_whenMember_thenThrows() {
        AuthContext auth = new AuthContext(2L, 10L, RoleType.MEMBER);
        CreateMissionRequest request =
                new CreateMissionRequest("clean room", 2L, 500L, "DATA", 100L);

        assertThatThrownBy(() -> missionService.createMission(auth, request))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(MissionErrorCode.MISSION_OWNER_ONLY));
    }
}
