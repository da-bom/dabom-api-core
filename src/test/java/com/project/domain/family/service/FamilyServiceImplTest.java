package com.project.domain.family.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import com.project.domain.customer.enums.RoleType;
import com.project.domain.family.dto.request.FamilySearchRequest;
import com.project.domain.family.entity.Family;
import com.project.domain.family.infra.cache.FamilyCacheRepository;
import com.project.domain.family.model.FamilyDetail;
import com.project.domain.family.model.FamilyMemberDetail;
import com.project.domain.family.model.FamilyMemberSummary;
import com.project.domain.family.model.FamilySearchResult;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.family.repository.FamilyQueryRepository;
import com.project.domain.family.repository.FamilyRepository;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.FamilyErrorCode;

@ExtendWith(MockitoExtension.class)
class FamilyServiceImplTest {

    @Mock private FamilyQueryRepository familyQueryRepository;
    @Mock private FamilyCacheRepository familyCacheRepository;
    @Mock private FamilyMemberRepository familyMemberRepository;
    @Mock private FamilyRepository familyRepository;

    @InjectMocks private FamilyServiceImpl familyService;

    @Test
    @DisplayName("searchFamilies - 가족 검색 결과를 반환한다")
    void searchFamilies_validRequest_returnsFamilyList() {
        // given
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
        given(familyQueryRepository.search(request)).willReturn(expected);

        // when
        Page<FamilySearchResult> actual = familyService.searchFamilies(request);

        // then
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.getContent()).hasSize(2);
        verify(familyQueryRepository).search(request);
    }

    @Test
    @DisplayName("getFamilyDetail - 캐시 사용량으로 보정하여 반환한다")
    void getFamilyDetail_validFamilyId_returnsAdjustedFamilyDetail() {
        // given
        Long familyId = 100L;
        FamilyDetail dbDetail =
                new FamilyDetail(
                        familyId,
                        "다봄 가족",
                        1L,
                        List.of(
                                new FamilyMemberDetail(1L, "아빠", RoleType.OWNER, 5000L, 1000L),
                                new FamilyMemberDetail(2L, "엄마", RoleType.OWNER, 3000L, 800L),
                                new FamilyMemberDetail(3L, "아이", RoleType.MEMBER, 2000L, null)),
                        10_000L,
                        1_800L,
                        18.0,
                        LocalDate.of(2026, 2, 1),
                        LocalDateTime.of(2026, 2, 1, 10, 0),
                        LocalDateTime.of(2026, 2, 1, 10, 0));
        given(familyQueryRepository.findDetailById(familyId)).willReturn(Optional.of(dbDetail));
        given(familyCacheRepository.findCustomerMonthlyUsageBytes(familyId, 1L))
                .willReturn(Optional.of(1_500L));
        given(familyCacheRepository.findCustomerMonthlyUsageBytes(familyId, 2L))
                .willReturn(Optional.empty());
        given(familyCacheRepository.findCustomerMonthlyUsageBytes(familyId, 3L))
                .willReturn(Optional.of(100L));

        // when
        FamilyDetail actual = familyService.getFamilyDetail(familyId);

        // then
        assertThat(actual.usedBytes()).isEqualTo(2_400L);
        assertThat(actual.usedPercent()).isCloseTo(24.0, within(0.01));
        assertThat(actual.customers()).hasSize(3);
        assertThat(actual.customers().get(0).monthlyUsedBytes()).isEqualTo(1_500L);
        assertThat(actual.customers().get(1).monthlyUsedBytes()).isEqualTo(800L);
        assertThat(actual.customers().get(2).monthlyUsedBytes()).isEqualTo(100L);
    }

    @Test
    @DisplayName("updateFamilyName - 가족 이름이 정상적으로 변경된다")
    void updateFamilyName_validCustomer_changesName() {
        // given
        Long customerId = 1L;
        Long familyId = 100L;
        String newName = "김씨 가족";

        Family family =
                Family.builder()
                        .name("다봄 가족")
                        .createdById(customerId)
                        .totalQuotaBytes(10_000L)
                        .usedBytes(3_000L)
                        .currentMonth(LocalDate.now().withDayOfMonth(1))
                        .build();

        given(familyMemberRepository.findFamilyIdByCustomerId(customerId))
                .willReturn(Optional.of(familyId));
        given(familyRepository.findById(familyId)).willReturn(Optional.of(family));

        // when
        Family result = familyService.updateFamilyName(customerId, newName);

        // then
        assertThat(result.getName()).isEqualTo(newName);
        verify(familyMemberRepository).findFamilyIdByCustomerId(customerId);
        verify(familyRepository).findById(familyId);
    }

    @Test
    @DisplayName("updateFamilyName - 고객이 가족에 속하지 않으면 예외를 던진다")
    void updateFamilyName_customerNotInFamily_throwsException() {
        // given
        Long customerId = 9_999L;
        given(familyMemberRepository.findFamilyIdByCustomerId(customerId))
                .willReturn(Optional.empty());

        // when & then
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
        // given
        Long customerId = 1L;
        Long familyId = 9_999L;
        given(familyMemberRepository.findFamilyIdByCustomerId(customerId))
                .willReturn(Optional.of(familyId));
        given(familyRepository.findById(familyId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> familyService.updateFamilyName(customerId, "새 이름"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(FamilyErrorCode.FAMILY_NOT_FOUND));
    }

    @Test
    @DisplayName("getFamilyDetail - 가족이 없으면 예외를 던진다")
    void getFamilyDetail_notFoundFamily_throwsException() {
        // given
        Long familyId = 9_999L;
        given(familyQueryRepository.findDetailById(familyId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> familyService.getFamilyDetail(familyId))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        exception ->
                                assertThat(((ApplicationException) exception).getCode())
                                        .isEqualTo(FamilyErrorCode.FAMILY_NOT_FOUND));
    }
}
