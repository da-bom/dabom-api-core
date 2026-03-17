package com.project.domain.family.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import com.project.common.auth.enums.RoleType;
import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.FamilyErrorCode;
import com.project.domain.family.dto.request.FamilySearchRequest;
import com.project.domain.family.entity.Family;
import com.project.domain.family.model.FamilyDetail;
import com.project.domain.family.model.FamilyMemberDetail;
import com.project.domain.family.model.FamilyMemberInfo;
import com.project.domain.family.model.FamilyMemberSummary;
import com.project.domain.family.model.FamilySearchResult;
import com.project.domain.customer.repository.CustomerQuotaRepository;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.family.repository.FamilyQueryRepository;
import com.project.domain.family.repository.FamilyRepository;
import com.project.domain.policy.repository.PolicyAssignmentRepository;

@ExtendWith(MockitoExtension.class)
class FamilyServiceImplTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-03-14T15:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDate TARGET_MONTH = LocalDate.of(2026, 3, 1);

    @Mock private FamilyQueryRepository familyQueryRepository;
    @Mock private FamilyMemberRepository familyMemberRepository;
    @Mock private FamilyRepository familyRepository;
    @Mock private CustomerQuotaRepository customerQuotaRepository;
    @Mock private PolicyAssignmentRepository policyAssignmentRepository;

    private FamilyServiceImpl familyService;

    @BeforeEach
    void setUp() {
        familyService =
                new FamilyServiceImpl(
                        familyQueryRepository,
                        familyMemberRepository,
                        familyRepository,
                        customerQuotaRepository,
                        policyAssignmentRepository,
                        FIXED_CLOCK);
    }

    @Test
    @DisplayName("searchFamilies - 가족 검색 결과를 반환한다")
    void searchFamilies_validRequest_returnsFamilyList() {
        FamilySearchRequest request =
                new FamilySearchRequest(
                        0,
                        20,
                        null,
                        List.of(new FamilySearchRequest.SortCondition("createdAt", "desc")));

        FamilySearchResult result =
                new FamilySearchResult(
                        100L,
                        "다봄 가족",
                        List.of(new FamilyMemberSummary(1L, "아빠")),
                        LocalDateTime.of(2026, 2, 1, 12, 0));
        FamilySearchResult secondResult =
                new FamilySearchResult(
                        200L,
                        "다봄 가족2",
                        List.of(new FamilyMemberSummary(1L, "아빠")),
                        LocalDateTime.of(2026, 2, 2, 12, 0));
        Page<FamilySearchResult> expected = new PageImpl<>(List.of(result, secondResult));
        given(familyQueryRepository.search(request, TARGET_MONTH)).willReturn(expected);

        Page<FamilySearchResult> actual = familyService.searchFamilies(request);

        assertThat(actual).isEqualTo(expected);
        assertThat(actual.getContent()).hasSize(2);
        verify(familyQueryRepository).search(request, TARGET_MONTH);
    }

    @Test
    @DisplayName("getFamilyDetail - DB 사용량 기준으로 반환한다")
    void getFamilyDetail_validFamilyId_returnsFamilyDetail() {
        Long familyId = 100L;
        FamilyDetail dbDetail =
                new FamilyDetail(
                        familyId,
                        "다봄 가족",
                        1L,
                        List.of(
                                new FamilyMemberDetail(1L, "아빠", RoleType.OWNER, 5000L, 1200L),
                                new FamilyMemberDetail(2L, "엄마", RoleType.OWNER, 3000L, 800L),
                                new FamilyMemberDetail(3L, "아이", RoleType.MEMBER, 2000L, 500L)),
                        10_000L,
                        2_500L,
                        25.0,
                        TARGET_MONTH,
                        LocalDateTime.of(2026, 2, 1, 10, 0),
                        LocalDateTime.of(2026, 2, 1, 10, 0));
        given(familyQueryRepository.findDetailById(familyId, TARGET_MONTH))
                .willReturn(Optional.of(dbDetail));

        FamilyDetail actual = familyService.getFamilyDetail(familyId);

        assertThat(actual.usedBytes()).isEqualTo(2_500L);
        assertThat(actual.usedPercent()).isCloseTo(25.0, within(0.01));
        assertThat(actual.currentMonth()).isEqualTo(TARGET_MONTH);
        assertThat(actual.customers()).hasSize(3);
        assertThat(actual.customers().get(0).monthlyUsedBytes()).isEqualTo(1_200L);
        assertThat(actual.customers().get(1).monthlyUsedBytes()).isEqualTo(800L);
        assertThat(actual.customers().get(2).monthlyUsedBytes()).isEqualTo(500L);
    }

    @Test
    @DisplayName("updateFamilyName - 가족 이름이 정상적으로 변경된다")
    void updateFamilyName_validCustomer_changesName() {
        Long customerId = 1L;
        Long familyId = 100L;
        String newName = "김씨 가족";

        Family family = Family.builder().name("다봄 가족").createdById(customerId).build();

        given(familyMemberRepository.findFamilyIdByCustomerId(customerId))
                .willReturn(Optional.of(familyId));
        given(familyRepository.findById(familyId)).willReturn(Optional.of(family));

        Family result = familyService.updateFamilyName(customerId, newName);

        assertThat(result.getName()).isEqualTo(newName);
        verify(familyMemberRepository).findFamilyIdByCustomerId(customerId);
        verify(familyRepository).findById(familyId);
    }

    @Test
    @DisplayName("updateFamilyName - 고객이 가족에 속하지 않으면 예외를 던진다")
    void updateFamilyName_customerNotInFamily_throwsException() {
        Long customerId = 9_999L;
        given(familyMemberRepository.findFamilyIdByCustomerId(customerId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> familyService.updateFamilyName(customerId, "새 이름"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(FamilyErrorCode.FAMILY_NOT_FOUND));
    }

    @Test
    @DisplayName("updateFamilyName - 가족 엔티티가 존재하지 않으면 예외를 던진다")
    void updateFamilyName_familyNotFound_throwsException() {
        Long customerId = 1L;
        Long familyId = 9_999L;
        given(familyMemberRepository.findFamilyIdByCustomerId(customerId))
                .willReturn(Optional.of(familyId));
        given(familyRepository.findById(familyId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> familyService.updateFamilyName(customerId, "새 이름"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(FamilyErrorCode.FAMILY_NOT_FOUND));
    }

    @Test
    @DisplayName("getFamilyMembers - MEMBER 역할의 가족 구성원만 반환한다")
    void getFamilyMembers_validCustomer_returnsMembersOnly() {
        Long customerId = 1L;
        Long familyId = 100L;
        List<FamilyMemberInfo> members =
                List.of(
                        new FamilyMemberInfo(2L, "아이1", RoleType.MEMBER),
                        new FamilyMemberInfo(3L, "아이2", RoleType.MEMBER));

        given(familyMemberRepository.findFamilyIdByCustomerId(customerId))
                .willReturn(Optional.of(familyId));
        given(familyQueryRepository.findMembersByFamilyId(familyId)).willReturn(members);

        List<FamilyMemberInfo> result = familyService.getFamilyMembers(customerId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(FamilyMemberInfo::role).containsOnly(RoleType.MEMBER);
        assertThat(result).extracting(FamilyMemberInfo::name).containsExactly("아이1", "아이2");
        verify(familyMemberRepository).findFamilyIdByCustomerId(customerId);
        verify(familyQueryRepository).findMembersByFamilyId(familyId);
    }

    @Test
    @DisplayName("getFamilyMembers - 고객이 가족에 속하지 않으면 예외를 던진다")
    void getFamilyMembers_customerNotInFamily_throwsException() {
        Long customerId = 9_999L;
        given(familyMemberRepository.findFamilyIdByCustomerId(customerId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> familyService.getFamilyMembers(customerId))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(FamilyErrorCode.FAMILY_NOT_FOUND));
    }

    @Test
    @DisplayName("getFamilyDetail - 가족이 없으면 예외를 던진다")
    void getFamilyDetail_notFoundFamily_throwsException() {
        Long familyId = 9_999L;
        given(familyQueryRepository.findDetailById(familyId, TARGET_MONTH))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> familyService.getFamilyDetail(familyId))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        exception ->
                                assertThat(((ApplicationException) exception).getCode())
                                        .isEqualTo(FamilyErrorCode.FAMILY_NOT_FOUND));
    }
}
