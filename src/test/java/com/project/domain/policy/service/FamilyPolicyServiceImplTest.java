package com.project.domain.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.policy.dto.response.FamilyPolicyResponse;
import com.project.domain.policy.repository.PolicyAssignmentRepository;
import com.project.domain.policy.repository.PolicyQueryRepository;

@ExtendWith(MockitoExtension.class)
class FamilyPolicyServiceImplTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-03-14T15:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDate CURRENT_MONTH = LocalDate.of(2026, 3, 1);

    @Mock private PolicyAssignmentRepository policyAssignmentRepository;
    @Mock private PolicyQueryRepository policyQueryRepository;
    @Mock private FamilyMemberRepository familyMemberRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private PolicyRedisService policyRedisService;

    private FamilyPolicyServiceImpl familyPolicyService;

    @BeforeEach
    void setUp() {
        familyPolicyService =
                new FamilyPolicyServiceImpl(
                        policyAssignmentRepository,
                        policyQueryRepository,
                        familyMemberRepository,
                        objectMapper,
                        policyRedisService,
                        FIXED_CLOCK);
    }

    @Test
    @DisplayName("getFamilyPolicyResponse - 현재월 targetMonth를 repository에 전달한다")
    void getFamilyPolicyResponse_passesCurrentMonthToRepository() {
        Long customerId = 10L;
        List<FamilyPolicyResponse.FlatPolicyRow> rows =
                List.of(
                        new FamilyPolicyResponse.FlatPolicyRow(
                                100L,
                                10L,
                                "아빠",
                                "01011112222",
                                "OWNER",
                                1_200L,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null));
        given(policyQueryRepository.findAllFamilyPoliciesByCustomerId(customerId, CURRENT_MONTH))
                .willReturn(rows);

        FamilyPolicyResponse actual = familyPolicyService.getFamilyPolicyResponse(customerId);

        assertThat(actual.familyId()).isEqualTo(100L);
        assertThat(actual.customers()).hasSize(1);
        verify(policyQueryRepository).findAllFamilyPoliciesByCustomerId(customerId, CURRENT_MONTH);
    }
}
