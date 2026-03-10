package com.project.domain.appeal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.project.domain.appeal.entity.PolicyAppeal;
import com.project.domain.appeal.enums.AppealStatus;
import com.project.domain.appeal.enums.AppealType;
import com.project.domain.appeal.model.AppealListResult;
import com.project.domain.appeal.repository.PolicyAppealRepository;
import com.project.domain.customer.entity.Customer;
import com.project.domain.customer.enums.RoleType;
import com.project.domain.customer.repository.CustomerRepository;
import com.project.global.auth.model.AuthContext;

@ExtendWith(MockitoExtension.class)
class AppealServiceImplTest {

    @Mock private PolicyAppealRepository policyAppealRepository;
    @Mock private CustomerRepository customerRepository;

    private AppealServiceImpl appealService;

    @BeforeEach
    void setUp() {
        appealService = new AppealServiceImpl(policyAppealRepository, customerRepository);
    }

    @Test
    @DisplayName("OWNER는 가족 전체 이의제기를 조회한다")
    void getAppeals_whenOwner_thenReturnsFamilyAppeals() {
        AuthContext auth = new AuthContext(1L, 10L, RoleType.OWNER);
        PolicyAppeal first = appeal(30L, 2L, 100L, AppealStatus.PENDING);
        PolicyAppeal second = appeal(29L, 3L, 101L, AppealStatus.APPROVED);
        given(policyAppealRepository.findAllByFamilyId(10L, null, null, PageRequest.of(0, 21)))
                .willReturn(List.of(first, second));
        given(customerRepository.findAllById(anyIterable()))
                .willReturn(List.of(customer(2L, "member-a"), customer(3L, "member-b")));

        AppealListResult result = appealService.getAppeals(auth, null, null, 20);

        assertThat(result.appeals()).hasSize(2);
        assertThat(result.appeals().get(0).requesterName()).isEqualTo("member-a");
        assertThat(result.appeals().get(1).policyAssignmentId()).isEqualTo(101L);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    @DisplayName("MEMBER는 본인 이의제기만 조회한다")
    void getAppeals_whenMember_thenQueriesByRequester() {
        AuthContext auth = new AuthContext(2L, 10L, RoleType.MEMBER);
        PolicyAppeal appeal = appeal(30L, 2L, 100L, AppealStatus.PENDING);
        given(
                        policyAppealRepository.findByRequesterIdAndFamilyId(
                                2L, 10L, AppealStatus.PENDING, null, PageRequest.of(0, 21)))
                .willReturn(List.of(appeal));
        given(customerRepository.findAllById(anyIterable()))
                .willReturn(List.of(customer(2L, "member-a")));

        AppealListResult result = appealService.getAppeals(auth, AppealStatus.PENDING, null, 20);

        assertThat(result.appeals()).hasSize(1);
        assertThat(result.appeals().getFirst().requesterId()).isEqualTo(2L);
        assertThat(result.appeals().getFirst().status()).isEqualTo(AppealStatus.PENDING);
        verify(policyAppealRepository)
                .findByRequesterIdAndFamilyId(
                        2L, 10L, AppealStatus.PENDING, null, PageRequest.of(0, 21));
    }

    @Test
    @DisplayName("목록 조회는 커서 기반 nextCursor와 hasNext를 계산한다")
    void getAppeals_whenMoreThanSize_thenReturnsNextCursor() {
        AuthContext auth = new AuthContext(1L, 10L, RoleType.OWNER);
        given(policyAppealRepository.findAllByFamilyId(10L, null, 50L, PageRequest.of(0, 3)))
                .willReturn(
                        List.of(
                                appeal(40L, 2L, 100L, AppealStatus.PENDING),
                                appeal(39L, 2L, 101L, AppealStatus.PENDING),
                                appeal(38L, 2L, 102L, AppealStatus.PENDING)));
        given(customerRepository.findAllById(anyIterable()))
                .willReturn(List.of(customer(2L, "member-a")));

        AppealListResult result = appealService.getAppeals(auth, null, 50L, 2);

        assertThat(result.appeals()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo("39");
    }

    private PolicyAppeal appeal(
            Long appealId, Long requesterId, Long policyAssignmentId, AppealStatus status) {
        PolicyAppeal appeal =
                PolicyAppeal.builder()
                        .id(appealId)
                        .type(AppealType.NORMAL)
                        .policyAssignmentId(policyAssignmentId)
                        .requesterId(requesterId)
                        .requestReason("need change")
                        .desiredRules(Map.of("limitBytes", 1024L))
                        .status(status)
                        .build();
        setCreatedAt(appeal, LocalDateTime.of(2026, 3, 10, 10, 0));
        return appeal;
    }

    private Customer customer(Long customerId, String name) {
        Customer customer = new Customer("01012345678", "hash", name);
        setField(customer, Customer.class, "id", customerId);
        return customer;
    }

    private void setCreatedAt(PolicyAppeal appeal, LocalDateTime createdAt) {
        setField(appeal, appeal.getClass().getSuperclass(), "createdAt", createdAt);
    }

    private void setField(Object target, Class<?> type, String fieldName, Object value) {
        try {
            var field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
