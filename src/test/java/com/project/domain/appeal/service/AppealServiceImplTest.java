package com.project.domain.appeal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.project.domain.appeal.dto.request.AppealCommentRequest;
import com.project.domain.appeal.dto.request.AppealCreateRequest;
import com.project.domain.appeal.dto.request.AppealRespondRequest;
import com.project.domain.appeal.dto.request.EmergencyQuotaRequest;
import com.project.domain.appeal.entity.PolicyAppeal;
import com.project.domain.appeal.entity.PolicyAppealComment;
import com.project.domain.appeal.enums.AppealStatus;
import com.project.domain.appeal.enums.AppealType;
import com.project.domain.appeal.model.AppealCreateResult;
import com.project.domain.appeal.model.AppealDetailResult;
import com.project.domain.appeal.model.AppealListResult;
import com.project.domain.appeal.model.AppealRespondResult;
import com.project.domain.appeal.model.EmergencyQuotaResult;
import com.project.domain.appeal.repository.PolicyAppealCommentRepository;
import com.project.domain.appeal.repository.PolicyAppealRepository;
import com.project.domain.customer.entity.Customer;
import com.project.domain.customer.entity.CustomerQuota;
import com.project.domain.customer.enums.RoleType;
import com.project.domain.customer.repository.CustomerQuotaRepository;
import com.project.domain.customer.repository.CustomerRepository;
import com.project.domain.family.entity.FamilyMember;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.policy.entity.Policy;
import com.project.domain.policy.entity.PolicyAssignment;
import com.project.domain.policy.enums.PolicyType;
import com.project.domain.policy.model.AppliedPolicyQueryResult;
import com.project.domain.policy.repository.PolicyAssignmentRepository;
import com.project.domain.policy.repository.PolicyRepository;
import com.project.common.auth.model.AuthContext;
import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.AppealErrorCode;

@ExtendWith(MockitoExtension.class)
class AppealServiceImplTest {

    private static final ZoneId ASIA_SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-03-11T00:00:00Z"), ASIA_SEOUL);

    @Mock private PolicyAppealRepository policyAppealRepository;
    @Mock private PolicyAppealCommentRepository policyAppealCommentRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private FamilyMemberRepository familyMemberRepository;
    @Mock private PolicyAssignmentRepository policyAssignmentRepository;
    @Mock private PolicyRepository policyRepository;
    @Mock private CustomerQuotaRepository customerQuotaRepository;
    @Mock private ObjectMapper objectMapper;

    private AppealServiceImpl appealService;

    @BeforeEach
    void setUp() {
        appealService =
                new AppealServiceImpl(
                        FIXED_CLOCK,
                        policyAppealRepository,
                        policyAppealCommentRepository,
                        customerRepository,
                        familyMemberRepository,
                        policyAssignmentRepository,
                        policyRepository,
                        customerQuotaRepository,
                        objectMapper);
    }

    @Test
    @DisplayName("현재 사용자에게 적용 중인 정책 목록을 이의제기용 정책 목록으로 변환한다")
    void getAppealablePolicyListResult_whenPoliciesExist_thenReturnsMappedPolicies()
            throws JsonProcessingException {
        AuthContext auth = new AuthContext(2L, 10L, RoleType.MEMBER);
        AppliedPolicyQueryResult first =
                new AppliedPolicyQueryResult(
                        100L,
                        50L,
                        "야간 사용 제한",
                        PolicyType.TIME_BLOCK,
                        "{\"startTime\":\"22:00\",\"endTime\":\"07:00\"}",
                        true,
                        LocalDateTime.of(2026, 3, 10, 22, 0));
        AppliedPolicyQueryResult second =
                new AppliedPolicyQueryResult(
                        101L,
                        51L,
                        "월간 데이터 제한",
                        PolicyType.MONTHLY_LIMIT,
                        "{\"limitBytes\":2048}",
                        true,
                        LocalDateTime.of(2026, 3, 1, 0, 0));

        given(policyAssignmentRepository.findAppealablePoliciesByCustomerId(2L))
                .willReturn(List.of(first, second));
        given(
                        objectMapper.readValue(
                                org.mockito.ArgumentMatchers.eq(
                                        "{\"startTime\":\"22:00\",\"endTime\":\"07:00\"}"),
                                any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .willReturn(Map.of("startTime", "22:00", "endTime", "07:00"));
        given(
                        objectMapper.readValue(
                                org.mockito.ArgumentMatchers.eq("{\"limitBytes\":2048}"),
                                any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .willReturn(Map.of("limitBytes", 2048));

        var result = appealService.getAppealablePolicyListResult(auth);

        assertThat(result.policies()).hasSize(2);
        assertThat(result.policies().get(0).policyAssignmentId()).isEqualTo(100L);
        assertThat(result.policies().get(0).policyId()).isEqualTo(50L);
        assertThat(result.policies().get(0).policyName()).isEqualTo("야간 사용 제한");
        assertThat(result.policies().get(0).policyType()).isEqualTo(PolicyType.TIME_BLOCK);
        assertThat(result.policies().get(0).appliedRules()).containsEntry("startTime", "22:00");
        assertThat(result.policies().get(1).appliedRules()).containsEntry("limitBytes", 2048);
    }

    @Test
    @DisplayName("OWNER는 가족 전체 이의제기를 조회한다")
    void getAppeals_whenOwner_thenReturnsFamilyAppeals() {
        AuthContext auth = new AuthContext(1L, 10L, RoleType.OWNER, "owner");
        PolicyAppeal first = appeal(30L, 2L, 100L, AppealStatus.PENDING);
        PolicyAppeal second = appeal(29L, 3L, 101L, AppealStatus.APPROVED);
        given(policyAppealRepository.findAllByFamilyId(10L, null, null, PageRequest.of(0, 21)))
                .willReturn(List.of(first, second));
        given(customerRepository.findAllById(anyIterable()))
                .willReturn(List.of(customer(2L, "member-a"), customer(3L, "member-b")));
        given(policyAssignmentRepository.findAllByIdInAndDeletedAtIsNull(Set.of(100L, 101L)))
                .willReturn(
                        List.of(
                                policyAssignment(100L, 50L, 10L, 2L),
                                policyAssignment(101L, 51L, 10L, 3L)));
        given(policyRepository.findAllByIdInAndDeletedAtIsNull(Set.of(50L, 51L)))
                .willReturn(
                        List.of(
                                policy(50L, PolicyType.MONTHLY_LIMIT),
                                policy(51L, PolicyType.TIME_BLOCK)));

        AppealListResult result = appealService.getAppeals(auth, null, null, 20);

        assertThat(result.appeals()).hasSize(2);
        assertThat(result.appeals().get(0).requesterName()).isEqualTo("member-a");
        assertThat(result.appeals().get(0).policyType()).isEqualTo(PolicyType.MONTHLY_LIMIT);
        assertThat(result.appeals().get(1).policyAssignmentId()).isEqualTo(101L);
        assertThat(result.appeals().get(1).policyType()).isEqualTo(PolicyType.TIME_BLOCK);
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
        given(policyAssignmentRepository.findAllByIdInAndDeletedAtIsNull(Set.of(100L)))
                .willReturn(List.of(policyAssignment(100L, 50L, 10L, 2L)));
        given(policyRepository.findAllByIdInAndDeletedAtIsNull(Set.of(50L)))
                .willReturn(List.of(policy(50L, PolicyType.MONTHLY_LIMIT)));

        AppealListResult result = appealService.getAppeals(auth, AppealStatus.PENDING, null, 20);

        assertThat(result.appeals()).hasSize(1);
        assertThat(result.appeals().getFirst().requesterId()).isEqualTo(2L);
        assertThat(result.appeals().getFirst().policyType()).isEqualTo(PolicyType.MONTHLY_LIMIT);
        assertThat(result.appeals().getFirst().status()).isEqualTo(AppealStatus.PENDING);
        verify(policyAppealRepository)
                .findByRequesterIdAndFamilyId(
                        2L, 10L, AppealStatus.PENDING, null, PageRequest.of(0, 21));
    }

    @Test
    @DisplayName("목록 조회는 커서 기반 nextCursor와 hasNext를 계산한다")
    void getAppeals_whenMoreThanSize_thenReturnsNextCursor() {
        AuthContext auth = new AuthContext(1L, 10L, RoleType.OWNER, "owner");
        given(policyAppealRepository.findAllByFamilyId(10L, null, 50L, PageRequest.of(0, 3)))
                .willReturn(
                        List.of(
                                appeal(40L, 2L, 100L, AppealStatus.PENDING),
                                appeal(39L, 2L, 101L, AppealStatus.PENDING),
                                appeal(38L, 2L, 102L, AppealStatus.PENDING)));
        given(customerRepository.findAllById(anyIterable()))
                .willReturn(List.of(customer(2L, "member-a")));
        given(policyAssignmentRepository.findAllByIdInAndDeletedAtIsNull(Set.of(100L, 101L)))
                .willReturn(
                        List.of(
                                policyAssignment(100L, 50L, 10L, 2L),
                                policyAssignment(101L, 51L, 10L, 2L)));
        given(policyRepository.findAllByIdInAndDeletedAtIsNull(Set.of(50L, 51L)))
                .willReturn(
                        List.of(
                                policy(50L, PolicyType.MONTHLY_LIMIT),
                                policy(51L, PolicyType.TIME_BLOCK)));

        AppealListResult result = appealService.getAppeals(auth, null, 50L, 2);

        assertThat(result.appeals()).hasSize(2);
        assertThat(result.appeals().get(0).policyType()).isEqualTo(PolicyType.MONTHLY_LIMIT);
        assertThat(result.appeals().get(1).policyType()).isEqualTo(PolicyType.TIME_BLOCK);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo("39");
    }

    @Test
    @DisplayName("EMERGENCY 이의제기 목록은 policyType을 null로 반환한다")
    void getAppeals_whenEmergencyAppeal_thenReturnsNullPolicyType() {
        AuthContext auth = new AuthContext(1L, 10L, RoleType.OWNER, "owner");
        PolicyAppeal emergencyAppeal = appeal(41L, 2L, null, AppealStatus.APPROVED);
        given(policyAppealRepository.findAllByFamilyId(10L, null, null, PageRequest.of(0, 21)))
                .willReturn(List.of(emergencyAppeal));
        given(customerRepository.findAllById(anyIterable()))
                .willReturn(List.of(customer(2L, "member-a")));

        AppealListResult result = appealService.getAppeals(auth, null, null, 20);

        assertThat(result.appeals()).hasSize(1);
        assertThat(result.appeals().getFirst().policyAssignmentId()).isNull();
        assertThat(result.appeals().getFirst().policyType()).isNull();
    }

    @Test
    @DisplayName("상세 조회는 같은 가족의 댓글 커서 페이지와 정책 타입을 반환한다")
    void getAppealDetail_whenSameFamily_thenReturnsDetail() {
        AuthContext auth = new AuthContext(1L, 10L, RoleType.OWNER, "owner");
        PolicyAppeal appeal = appeal(30L, 2L, 100L, AppealStatus.PENDING);
        PolicyAppealComment firstComment = comment(20L, 2L, "첫 댓글");
        PolicyAppealComment secondComment = comment(19L, 1L, "둘째 댓글");
        PolicyAppealComment thirdComment = comment(18L, 2L, "셋째 댓글");

        given(policyAppealRepository.findByIdAndDeletedAtIsNull(30L))
                .willReturn(java.util.Optional.of(appeal));
        given(familyMemberRepository.findByCustomerId(2L))
                .willReturn(
                        java.util.Optional.of(
                                FamilyMember.builder()
                                        .familyId(10L)
                                        .customerId(2L)
                                        .role(RoleType.MEMBER)
                                        .build()));
        given(
                        policyAppealCommentRepository.findByAppealIdAndIdLessThanOrderByIdDesc(
                                30L, null, PageRequest.of(0, 3)))
                .willReturn(List.of(firstComment, secondComment, thirdComment));
        given(policyAssignmentRepository.findByIdAndDeletedAtIsNull(100L))
                .willReturn(java.util.Optional.of(policyAssignment(100L, 50L, 10L, 2L)));
        given(policyRepository.findByIdAndDeletedAtIsNull(50L))
                .willReturn(java.util.Optional.of(policy(50L, PolicyType.MONTHLY_LIMIT)));
        given(customerRepository.findAllById(anyIterable()))
                .willReturn(List.of(customer(1L, "owner"), customer(2L, "member")));

        AppealDetailResult result = appealService.getAppealDetail(auth, 30L, null, 2);

        assertThat(result.policyType()).isEqualTo(PolicyType.MONTHLY_LIMIT);
        assertThat(result.comments().content()).hasSize(2);
        assertThat(result.comments().hasNext()).isTrue();
        assertThat(result.comments().nextCursor()).isEqualTo("19");
        assertThat(result.comments().content().getFirst().authorName()).isEqualTo("member");
    }

    @Test
    @DisplayName("MEMBER는 같은 가족 정책 할당으로 이의제기를 생성한다")
    void createAppeal_whenMember_thenCreatesAppeal() {
        AuthContext auth = new AuthContext(2L, 10L, RoleType.MEMBER);
        AppealCreateRequest request =
                new AppealCreateRequest(100L, "규칙 변경 요청", Map.of("limitBytes", 2048L));
        PolicyAssignment assignment = policyAssignment(100L, 50L, 10L, 2L);
        PolicyAppeal saved =
                PolicyAppeal.builder()
                        .id(40L)
                        .type(AppealType.NORMAL)
                        .policyAssignmentId(100L)
                        .requesterId(2L)
                        .requestReason("규칙 변경 요청")
                        .desiredRules(Map.of("limitBytes", 2048L))
                        .status(AppealStatus.PENDING)
                        .build();
        setCreatedAt(saved, LocalDateTime.of(2026, 3, 10, 10, 0));

        given(policyAssignmentRepository.findByIdAndDeletedAtIsNull(100L))
                .willReturn(java.util.Optional.of(assignment));
        given(
                        policyAppealRepository
                                .existsByPolicyAssignmentIdAndRequesterIdAndTypeAndStatusAndDeletedAtIsNull(
                                        100L, 2L, AppealType.NORMAL, AppealStatus.PENDING))
                .willReturn(false);
        given(policyAppealRepository.save(any(PolicyAppeal.class))).willReturn(saved);

        AppealCreateResult result = appealService.createAppeal(auth, request);

        assertThat(result.appealId()).isEqualTo(40L);
        assertThat(result.policyAssignmentId()).isEqualTo(100L);
        assertThat(result.status()).isEqualTo(AppealStatus.PENDING);
        assertThat(result.desiredRules()).containsEntry("limitBytes", 2048L);
    }

    @Test
    @DisplayName("OWNER는 일반 이의제기를 생성할 수 없다")
    void createAppeal_whenOwner_thenThrowsForbidden() {
        AuthContext auth = new AuthContext(1L, 10L, RoleType.OWNER);
        AppealCreateRequest request = new AppealCreateRequest(100L, "규칙 변경 요청", null);

        assertThatThrownBy(() -> appealService.createAppeal(auth, request))
                .isInstanceOf(ApplicationException.class)
                .extracting(ex -> ((ApplicationException) ex).getCode())
                .isEqualTo(AppealErrorCode.APPEAL_FORBIDDEN);
    }

    @Test
    @DisplayName("같은 정책에 대한 진행 중 이의제기가 있으면 생성할 수 없다")
    void createAppeal_whenPendingAppealExists_thenThrowsConflict() {
        AuthContext auth = new AuthContext(2L, 10L, RoleType.MEMBER);
        AppealCreateRequest request =
                new AppealCreateRequest(100L, "규칙 변경 요청", Map.of("limitBytes", 2048L));
        PolicyAssignment assignment = policyAssignment(100L, 50L, 10L, 2L);

        given(policyAssignmentRepository.findByIdAndDeletedAtIsNull(100L))
                .willReturn(java.util.Optional.of(assignment));
        given(
                        policyAppealRepository
                                .existsByPolicyAssignmentIdAndRequesterIdAndTypeAndStatusAndDeletedAtIsNull(
                                        100L, 2L, AppealType.NORMAL, AppealStatus.PENDING))
                .willReturn(true);

        assertThatThrownBy(() -> appealService.createAppeal(auth, request))
                .isInstanceOf(ApplicationException.class)
                .extracting(ex -> ((ApplicationException) ex).getCode())
                .isEqualTo(AppealErrorCode.APPEAL_ALREADY_PENDING);
    }

    @Test
    @DisplayName("OWNER는 PENDING 이의제기를 승인하고 desiredRules를 정책에 반영한다")
    void respondAppeal_whenApproved_thenUpdatesStatusAndAssignmentRules()
            throws JsonProcessingException {
        AuthContext auth = new AuthContext(1L, 10L, RoleType.OWNER);
        PolicyAppeal appeal = appeal(30L, 2L, 100L, AppealStatus.PENDING);
        PolicyAssignment assignment = policyAssignment(100L, 50L, 10L, 2L);

        given(policyAppealRepository.findByIdAndDeletedAtIsNull(30L))
                .willReturn(java.util.Optional.of(appeal));
        given(familyMemberRepository.findByCustomerId(2L))
                .willReturn(
                        java.util.Optional.of(
                                FamilyMember.builder()
                                        .familyId(10L)
                                        .customerId(2L)
                                        .role(RoleType.MEMBER)
                                        .build()));
        given(policyAssignmentRepository.findByIdAndDeletedAtIsNull(100L))
                .willReturn(java.util.Optional.of(assignment));
        given(objectMapper.writeValueAsString(appeal.getDesiredRules()))
                .willReturn("{\"limitBytes\":1024}");

        AppealRespondResult result =
                appealService.respondAppeal(auth, 30L, new AppealRespondRequest("APPROVED", null));

        assertThat(result.status()).isEqualTo(AppealStatus.APPROVED);
        assertThat(result.resolvedById()).isEqualTo(1L);
        assertThat(assignment.getRules()).isEqualTo("{\"limitBytes\":1024}");
    }

    @Test
    @DisplayName("OWNER는 PENDING 이의제기를 거절하고 rejectReason을 기록한다")
    void respondAppeal_whenRejected_thenStoresRejectReason() {
        AuthContext auth = new AuthContext(1L, 10L, RoleType.OWNER);
        PolicyAppeal appeal = appeal(30L, 2L, 100L, AppealStatus.PENDING);

        given(policyAppealRepository.findByIdAndDeletedAtIsNull(30L))
                .willReturn(java.util.Optional.of(appeal));
        given(familyMemberRepository.findByCustomerId(2L))
                .willReturn(
                        java.util.Optional.of(
                                FamilyMember.builder()
                                        .familyId(10L)
                                        .customerId(2L)
                                        .role(RoleType.MEMBER)
                                        .build()));

        AppealRespondResult result =
                appealService.respondAppeal(
                        auth, 30L, new AppealRespondRequest("REJECTED", "rules are not clear"));

        assertThat(result.status()).isEqualTo(AppealStatus.REJECTED);
        assertThat(result.rejectReason()).isEqualTo("rules are not clear");
        assertThat(result.resolvedById()).isEqualTo(1L);
    }

    @Test
    @DisplayName("MEMBER는 본인 NORMAL PENDING 이의제기만 취소할 수 있다")
    void cancelAppeal_whenOwnPendingNormal_thenCancels() {
        AuthContext auth = new AuthContext(2L, 10L, RoleType.MEMBER);
        PolicyAppeal appeal = appeal(30L, 2L, 100L, AppealStatus.PENDING);
        given(policyAppealRepository.findByIdAndDeletedAtIsNull(30L))
                .willReturn(java.util.Optional.of(appeal));

        var result = appealService.cancelAppeal(auth, 30L);

        assertThat(result.appealId()).isEqualTo(30L);
        assertThat(result.status()).isEqualTo(AppealStatus.CANCELLED);
        assertThat(result.cancelledAt()).isNotNull();
    }

    @Test
    @DisplayName("가족 구성원은 이의제기에 댓글을 작성할 수 있다")
    void createComment_whenFamilyMember_thenCreatesComment() {
        AuthContext auth = new AuthContext(1L, 10L, RoleType.OWNER, "owner");
        PolicyAppeal appeal = appeal(30L, 2L, 100L, AppealStatus.PENDING);
        PolicyAppealComment saved =
                PolicyAppealComment.builder()
                        .id(50L)
                        .appealId(30L)
                        .authorId(1L)
                        .comment("확인 후 처리할게요")
                        .build();
        setField(
                saved,
                saved.getClass().getSuperclass(),
                "createdAt",
                LocalDateTime.of(2026, 3, 10, 12, 0));

        given(policyAppealRepository.findByIdAndDeletedAtIsNull(30L))
                .willReturn(java.util.Optional.of(appeal));
        given(familyMemberRepository.findByCustomerId(2L))
                .willReturn(
                        java.util.Optional.of(
                                FamilyMember.builder()
                                        .familyId(10L)
                                        .customerId(2L)
                                        .role(RoleType.MEMBER)
                                        .build()));
        given(familyMemberRepository.findByCustomerId(1L))
                .willReturn(
                        java.util.Optional.of(
                                FamilyMember.builder()
                                        .familyId(10L)
                                        .customerId(1L)
                                        .role(RoleType.OWNER)
                                        .build()));
        given(policyAppealCommentRepository.save(any(PolicyAppealComment.class))).willReturn(saved);

        var result = appealService.createComment(auth, 30L, new AppealCommentRequest("확인 후 처리할게요"));

        assertThat(result.commentId()).isEqualTo(50L);
        assertThat(result.authorId()).isEqualTo(1L);
        assertThat(result.authorName()).isEqualTo("owner");
        assertThat(result.comment()).isEqualTo("확인 후 처리할게요");
    }

    @Test
    @DisplayName("긴급 쿼터 요청은 300MB를 즉시 증가시키고 PolicyAssignment rules도 동기화한다")
    void requestEmergencyQuota_whenValid_thenUpdatesQuotaAndPolicyAssignment()
            throws JsonProcessingException {
        AuthContext auth = new AuthContext(2L, 10L, RoleType.MEMBER);
        EmergencyQuotaRequest request = new EmergencyQuotaRequest("데이터가 부족합니다");
        CustomerQuota customerQuota =
                CustomerQuota.builder()
                        .familyId(10L)
                        .customerId(2L)
                        .monthlyLimitBytes(500_000_000L)
                        .monthlyUsedBytes(100L)
                        .currentMonth(LocalDate.now(FIXED_CLOCK).withDayOfMonth(1))
                        .isBlocked(false)
                        .build();
        PolicyAppeal saved =
                PolicyAppeal.builder()
                        .id(55L)
                        .type(AppealType.EMERGENCY)
                        .requesterId(2L)
                        .requestReason("데이터가 부족합니다")
                        .desiredRules(Map.of("additionalBytes", 314_572_800L))
                        .status(AppealStatus.APPROVED)
                        .build();
        setCreatedAt(saved, LocalDateTime.of(2026, 3, 10, 12, 0));

        PolicyAssignment assignment = policyAssignment(100L, 50L, 10L, 2L);

        given(
                        customerQuotaRepository
                                .findByFamilyIdAndCustomerIdAndCurrentMonthAndDeletedAtIsNull(
                                        10L, 2L, LocalDate.now(FIXED_CLOCK).withDayOfMonth(1)))
                .willReturn(java.util.Optional.of(customerQuota));
        given(policyAppealRepository.saveAndFlush(any(PolicyAppeal.class))).willReturn(saved);
        given(policyAssignmentRepository.findByTargetAndType(10L, 2L, PolicyType.MONTHLY_LIMIT))
                .willReturn(java.util.Optional.of(assignment));
        com.fasterxml.jackson.databind.type.TypeFactory typeFactory =
                com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance();
        MapType mapType = typeFactory.constructMapType(Map.class, String.class, Object.class);
        given(objectMapper.getTypeFactory()).willReturn(typeFactory);
        given(objectMapper.readValue("{\"limitBytes\":1024}", mapType))
                .willReturn(new java.util.HashMap<>(Map.of("limitBytes", 1024L)));
        given(objectMapper.writeValueAsString(Map.of("limitBytes", 814_572_800L)))
                .willReturn("{\"limitBytes\":814572800}");

        EmergencyQuotaResult result = appealService.requestEmergencyQuota(auth, request);

        assertThat(result.appealId()).isEqualTo(55L);
        assertThat(result.status()).isEqualTo(AppealStatus.APPROVED);
        assertThat(result.additionalBytes()).isEqualTo(314_572_800L);
        assertThat(result.newMonthlyLimitBytes()).isEqualTo(814_572_800L);
        assertThat(customerQuota.getMonthlyLimitBytes()).isEqualTo(814_572_800L);
        assertThat(assignment.getRules()).isEqualTo("{\"limitBytes\":814572800}");
    }

    @Test
    @DisplayName("긴급 쿼터 요청 시 MONTHLY_LIMIT 정책이 없으면 PolicyAssignment 동기화를 건너뛴다")
    void requestEmergencyQuota_whenNoPolicyAssignment_thenSkipsSync() {
        AuthContext auth = new AuthContext(2L, 10L, RoleType.MEMBER);
        EmergencyQuotaRequest request = new EmergencyQuotaRequest("데이터가 부족합니다");
        CustomerQuota customerQuota =
                CustomerQuota.builder()
                        .familyId(10L)
                        .customerId(2L)
                        .monthlyLimitBytes(500_000_000L)
                        .monthlyUsedBytes(100L)
                        .currentMonth(LocalDate.now(FIXED_CLOCK).withDayOfMonth(1))
                        .isBlocked(false)
                        .build();
        PolicyAppeal saved =
                PolicyAppeal.builder()
                        .id(55L)
                        .type(AppealType.EMERGENCY)
                        .requesterId(2L)
                        .requestReason("데이터가 부족합니다")
                        .desiredRules(Map.of("additionalBytes", 314_572_800L))
                        .status(AppealStatus.APPROVED)
                        .build();
        setCreatedAt(saved, LocalDateTime.of(2026, 3, 10, 12, 0));

        given(
                        customerQuotaRepository
                                .findByFamilyIdAndCustomerIdAndCurrentMonthAndDeletedAtIsNull(
                                        10L, 2L, LocalDate.now(FIXED_CLOCK).withDayOfMonth(1)))
                .willReturn(java.util.Optional.of(customerQuota));
        given(policyAppealRepository.saveAndFlush(any(PolicyAppeal.class))).willReturn(saved);
        given(policyAssignmentRepository.findByTargetAndType(10L, 2L, PolicyType.MONTHLY_LIMIT))
                .willReturn(java.util.Optional.empty());

        EmergencyQuotaResult result = appealService.requestEmergencyQuota(auth, request);

        assertThat(result.appealId()).isEqualTo(55L);
        assertThat(result.status()).isEqualTo(AppealStatus.APPROVED);
        assertThat(result.newMonthlyLimitBytes()).isEqualTo(814_572_800L);
    }

    @Test
    @DisplayName("이번 달 승인된 긴급 요청이 있으면 재요청을 거절한다")
    void requestEmergencyQuota_whenAlreadyApprovedThisMonth_thenThrowsLimit() {
        AuthContext auth = new AuthContext(2L, 10L, RoleType.MEMBER);
        EmergencyQuotaRequest request = new EmergencyQuotaRequest("데이터가 부족합니다");
        CustomerQuota customerQuota =
                CustomerQuota.builder()
                        .familyId(10L)
                        .customerId(2L)
                        .monthlyLimitBytes(500_000_000L)
                        .monthlyUsedBytes(100L)
                        .currentMonth(LocalDate.now(FIXED_CLOCK).withDayOfMonth(1))
                        .isBlocked(false)
                        .build();

        given(
                        customerQuotaRepository
                                .findByFamilyIdAndCustomerIdAndCurrentMonthAndDeletedAtIsNull(
                                        10L, 2L, LocalDate.now(FIXED_CLOCK).withDayOfMonth(1)))
                .willReturn(java.util.Optional.of(customerQuota));
        given(policyAppealRepository.saveAndFlush(any(PolicyAppeal.class)))
                .willThrow(new DataIntegrityViolationException("duplicate emergency grant month"));

        assertThatThrownBy(() -> appealService.requestEmergencyQuota(auth, request))
                .isInstanceOf(ApplicationException.class)
                .extracting(ex -> ((ApplicationException) ex).getCode())
                .isEqualTo(AppealErrorCode.APPEAL_EMERGENCY_MONTHLY_LIMIT);
    }

    private PolicyAppeal appeal(
            Long appealId, Long requesterId, Long policyAssignmentId, AppealStatus status) {
        PolicyAppeal appeal =
                PolicyAppeal.builder()
                        .id(appealId)
                        .type(policyAssignmentId == null ? AppealType.EMERGENCY : AppealType.NORMAL)
                        .policyAssignmentId(policyAssignmentId)
                        .requesterId(requesterId)
                        .requestReason("need change")
                        .desiredRules(Map.of("limitBytes", 1024L))
                        .status(status)
                        .build();
        setCreatedAt(appeal, LocalDateTime.of(2026, 3, 10, 10, 0));
        return appeal;
    }

    private PolicyAppealComment comment(Long commentId, Long authorId, String text) {
        PolicyAppealComment comment =
                PolicyAppealComment.builder()
                        .id(commentId)
                        .appealId(30L)
                        .authorId(authorId)
                        .comment(text)
                        .build();
        setField(
                comment,
                comment.getClass().getSuperclass(),
                "createdAt",
                LocalDateTime.of(2026, 3, 10, 11, 0));
        return comment;
    }

    private PolicyAssignment policyAssignment(
            Long assignmentId, Long policyId, Long familyId, Long targetCustomerId) {
        return PolicyAssignment.builder()
                .id(assignmentId)
                .policyId(policyId)
                .familyId(familyId)
                .targetCustomerId(targetCustomerId)
                .rules("{\"limitBytes\":1024}")
                .isActive(true)
                .build();
    }

    private Policy policy(Long policyId, PolicyType policyType) {
        return Policy.builder()
                .id(policyId)
                .name("월 한도")
                .policyType(policyType)
                .isSystem(true)
                .isActive(true)
                .build();
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
