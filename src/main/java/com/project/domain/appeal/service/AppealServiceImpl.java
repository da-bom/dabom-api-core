package com.project.domain.appeal.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.appeal.dto.request.AppealCreateRequest;
import com.project.domain.appeal.dto.request.EmergencyQuotaRequest;
import com.project.domain.appeal.entity.PolicyAppeal;
import com.project.domain.appeal.entity.PolicyAppealComment;
import com.project.domain.appeal.enums.AppealStatus;
import com.project.domain.appeal.enums.AppealType;
import com.project.domain.appeal.model.AppealCreateResult;
import com.project.domain.appeal.model.AppealDetailResult;
import com.project.domain.appeal.model.AppealListResult;
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
import com.project.domain.policy.repository.PolicyAssignmentRepository;
import com.project.domain.policy.repository.PolicyRepository;
import com.project.global.auth.model.AuthContext;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.AppealErrorCode;
import com.project.global.exception.code.CustomerErrorCode;
import com.project.global.exception.code.PolicyErrorCode;

import lombok.RequiredArgsConstructor;

/** 이의제기 서비스 구현 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppealServiceImpl implements AppealService {

    private static final int DEFAULT_CURSOR_SIZE = 20;
    private static final int MAX_CURSOR_SIZE = 100;
    private static final String UNKNOWN_NAME = "unknown";
    private static final long MIN_EMERGENCY_BYTES = 104_857_600L;
    private static final long MAX_EMERGENCY_BYTES = 314_572_800L;

    private final PolicyAppealRepository policyAppealRepository;
    private final PolicyAppealCommentRepository policyAppealCommentRepository;
    private final CustomerRepository customerRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final PolicyAssignmentRepository policyAssignmentRepository;
    private final PolicyRepository policyRepository;
    private final CustomerQuotaRepository customerQuotaRepository;

    /** 이의제기 목록 조회 */
    @Override
    public AppealListResult getAppeals(
            AuthContext auth, AppealStatus status, Long cursor, int size) {
        // 1. 요청 size를 기본값과 최대값 범위 안으로 보정한다.
        int pageSize = normalizeSize(size);

        // 2. 역할에 따라 OWNER는 가족 전체, MEMBER는 본인 이의제기만 조회한다.
        List<PolicyAppeal> appeals =
                auth.isOwner()
                        ? policyAppealRepository.findAllByFamilyId(
                                auth.familyId(), status, cursor, PageRequest.of(0, pageSize + 1))
                        : policyAppealRepository.findByRequesterIdAndFamilyId(
                                auth.customerId(),
                                auth.familyId(),
                                status,
                                cursor,
                                PageRequest.of(0, pageSize + 1));

        // 3. pageSize + 1 조회 결과로 다음 페이지 존재 여부와 다음 커서를 계산한다.
        boolean hasNext = appeals.size() > pageSize;
        List<PolicyAppeal> page = hasNext ? appeals.subList(0, pageSize) : appeals;
        String nextCursor = hasNext ? String.valueOf(page.getLast().getId()) : null;
        Map<Long, String> customerNameMap = loadCustomerNameMap(extractRequesterIds(page));

        // 4. 현재 페이지 엔티티를 응답 모델로 변환해 목록 결과를 반환한다.
        return new AppealListResult(
                page.stream().map(appeal -> toAppealSummary(appeal, customerNameMap)).toList(),
                nextCursor,
                hasNext);
    }

    /** 이의제기 상세 조회 */
    @Override
    public AppealDetailResult getAppealDetail(
            AuthContext auth, Long appealId, Long cursor, int size) {
        // 1. 이의제기 존재 여부와 가족 접근 가능 여부를 확인한다.
        PolicyAppeal appeal = findAppealOrThrow(appealId);
        validateFamilyAccess(auth.familyId(), appeal);

        // 2. 댓글 size를 보정하고 커서 기반으로 댓글을 조회한다.
        int pageSize = normalizeSize(size);
        List<PolicyAppealComment> comments =
                policyAppealCommentRepository.findByAppealIdAndIdLessThanOrderByIdDesc(
                        appealId, cursor, PageRequest.of(0, pageSize + 1));

        // 3. pageSize + 1 조회 결과로 댓글 nextCursor와 hasNext를 계산한다.
        boolean hasNext = comments.size() > pageSize;
        List<PolicyAppealComment> page = hasNext ? comments.subList(0, pageSize) : comments;
        String nextCursor = hasNext ? String.valueOf(page.getLast().getId()) : null;

        // 4. 요청자, 처리자, 댓글 작성자 이름과 정책 타입을 조회해 상세 응답으로 변환한다.
        Set<Long> customerIds = new HashSet<>();
        customerIds.add(appeal.getRequesterId());
        if (appeal.getResolvedById() != null) {
            customerIds.add(appeal.getResolvedById());
        }
        page.stream().map(PolicyAppealComment::getAuthorId).forEach(customerIds::add);

        Map<Long, String> customerNameMap = loadCustomerNameMap(customerIds);
        PolicyType policyType = resolvePolicyType(appeal);

        return new AppealDetailResult(
                appeal.getId(),
                appeal.getPolicyAssignmentId(),
                policyType,
                appeal.getRequesterId(),
                customerNameMap.getOrDefault(appeal.getRequesterId(), UNKNOWN_NAME),
                appeal.getRequestReason(),
                appeal.getRejectReason(),
                appeal.getDesiredRules(),
                appeal.getStatus(),
                appeal.getResolvedById(),
                appeal.getResolvedAt(),
                appeal.getCreatedAt(),
                new AppealDetailResult.CommentPage(
                        page.stream()
                                .map(comment -> toCommentItem(comment, customerNameMap))
                                .toList(),
                        nextCursor,
                        hasNext));
    }

    /** 이의제기 생성 */
    @Override
    @Transactional
    public AppealCreateResult createAppeal(AuthContext auth, AppealCreateRequest request) {
        // 1. MEMBER 역할만 일반 이의제기를 생성할 수 있다.
        validateMemberOnly(auth);

        // 2. 정책 할당이 존재하고 같은 가족 소속인지 확인한다.
        PolicyAssignment policyAssignment =
                policyAssignmentRepository
                        .findByIdAndDeletedAtIsNull(request.policyAssignmentId())
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                PolicyErrorCode.POLICY_ASSIGNMENT_NOT_FOUND));
        if (!policyAssignment.getFamilyId().equals(auth.familyId())) {
            throw new ApplicationException(AppealErrorCode.APPEAL_FORBIDDEN);
        }

        // 3. NORMAL/PENDING 상태의 이의제기를 저장한다.
        PolicyAppeal appeal =
                policyAppealRepository.save(
                        PolicyAppeal.builder()
                                .type(AppealType.NORMAL)
                                .policyAssignmentId(policyAssignment.getId())
                                .requesterId(auth.customerId())
                                .requestReason(request.requestReason())
                                .desiredRules(request.desiredRules())
                                .status(AppealStatus.PENDING)
                                .build());

        // 4. 생성 결과를 응답 모델로 반환한다.
        return new AppealCreateResult(
                appeal.getId(),
                appeal.getPolicyAssignmentId(),
                appeal.getStatus(),
                appeal.getDesiredRules(),
                appeal.getCreatedAt());
    }

    /** 긴급 쿼터 요청 */
    @Override
    @Transactional
    public EmergencyQuotaResult requestEmergencyQuota(
            AuthContext auth, EmergencyQuotaRequest request) {
        // 1. MEMBER 역할과 요청 바이트 범위를 검증한다.
        validateMemberOnly(auth);
        validateEmergencyBytes(request.additionalBytes());

        // 2. 현재 월에 승인된 긴급 요청이 이미 있는지 확인한다.
        LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime monthEnd = YearMonth.now().atEndOfMonth().atTime(23, 59, 59);
        boolean alreadyApproved =
                !policyAppealRepository
                        .findByRequesterIdAndTypeAndStatusAndCreatedAtBetween(
                                auth.customerId(),
                                AppealType.EMERGENCY,
                                AppealStatus.APPROVED,
                                monthStart,
                                monthEnd)
                        .isEmpty();
        if (alreadyApproved) {
            throw new ApplicationException(AppealErrorCode.APPEAL_EMERGENCY_MONTHLY_LIMIT);
        }

        // 3. 당월 고객 쿼터를 조회하고 무제한 사용자 여부를 확인한다.
        CustomerQuota customerQuota =
                customerQuotaRepository
                        .findByFamilyIdAndCustomerIdAndCurrentMonthAndDeletedAtIsNull(
                                auth.familyId(),
                                auth.customerId(),
                                LocalDate.now().withDayOfMonth(1))
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                CustomerErrorCode.CUSTOMER_NOT_FOUND));
        if (customerQuota.getMonthlyLimitBytes() == null) {
            throw new ApplicationException(AppealErrorCode.APPEAL_EMERGENCY_UNLIMITED);
        }

        // 4. APPROVED 상태의 긴급 이의제기를 저장하고 월 한도를 즉시 증가시킨다.
        PolicyAppeal appeal =
                policyAppealRepository.save(
                        PolicyAppeal.builder()
                                .type(AppealType.EMERGENCY)
                                .requesterId(auth.customerId())
                                .requestReason(request.requestReason())
                                .desiredRules(Map.of("additionalBytes", request.additionalBytes()))
                                .status(AppealStatus.APPROVED)
                                .build());
        customerQuota.addMonthlyLimitBytes(request.additionalBytes());

        // 5. 긴급 쿼터 결과를 응답 모델로 반환한다.
        return new EmergencyQuotaResult(
                appeal.getId(),
                appeal.getType(),
                appeal.getStatus(),
                request.additionalBytes(),
                customerQuota.getMonthlyLimitBytes(),
                appeal.getRequestReason(),
                appeal.getCreatedAt());
    }

    /** 이의제기 요약 모델 변환 */
    private AppealListResult.AppealSummary toAppealSummary(
            PolicyAppeal appeal, Map<Long, String> customerNameMap) {
        return new AppealListResult.AppealSummary(
                appeal.getId(),
                appeal.getType(),
                appeal.getPolicyAssignmentId(),
                appeal.getRequesterId(),
                customerNameMap.getOrDefault(appeal.getRequesterId(), UNKNOWN_NAME),
                appeal.getRequestReason(),
                appeal.getDesiredRules(),
                appeal.getStatus(),
                appeal.getCreatedAt());
    }

    /** 댓글 항목 모델 변환 */
    private AppealDetailResult.CommentItem toCommentItem(
            PolicyAppealComment comment, Map<Long, String> customerNameMap) {
        return new AppealDetailResult.CommentItem(
                comment.getId(),
                comment.getAuthorId(),
                customerNameMap.getOrDefault(comment.getAuthorId(), UNKNOWN_NAME),
                comment.getComment(),
                comment.getCreatedAt());
    }

    /** 이의제기 존재 여부 확인 */
    private PolicyAppeal findAppealOrThrow(Long appealId) {
        return policyAppealRepository
                .findByIdAndDeletedAtIsNull(appealId)
                .orElseThrow(() -> new ApplicationException(AppealErrorCode.APPEAL_NOT_FOUND));
    }

    /** 같은 가족 접근 가능 여부 검증 */
    private void validateFamilyAccess(Long familyId, PolicyAppeal appeal) {
        FamilyMember requester =
                familyMemberRepository
                        .findByCustomerId(appeal.getRequesterId())
                        .orElseThrow(
                                () -> new ApplicationException(AppealErrorCode.APPEAL_FORBIDDEN));
        if (!requester.getFamilyId().equals(familyId)) {
            throw new ApplicationException(AppealErrorCode.APPEAL_FORBIDDEN);
        }
    }

    /** MEMBER 전용 기능 여부 검증 */
    private void validateMemberOnly(AuthContext auth) {
        if (!RoleType.MEMBER.equals(auth.role())) {
            throw new ApplicationException(AppealErrorCode.APPEAL_FORBIDDEN);
        }
    }

    /** 긴급 요청 바이트 범위 검증 */
    private void validateEmergencyBytes(Long additionalBytes) {
        if (additionalBytes < MIN_EMERGENCY_BYTES || additionalBytes > MAX_EMERGENCY_BYTES) {
            throw new ApplicationException(AppealErrorCode.APPEAL_EMERGENCY_INVALID_BYTES);
        }
    }

    /** 정책 타입 조회 */
    private PolicyType resolvePolicyType(PolicyAppeal appeal) {
        if (AppealType.EMERGENCY.equals(appeal.getType())
                || appeal.getPolicyAssignmentId() == null) {
            return null;
        }

        PolicyAssignment assignment =
                policyAssignmentRepository
                        .findByIdAndDeletedAtIsNull(appeal.getPolicyAssignmentId())
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                PolicyErrorCode.POLICY_ASSIGNMENT_NOT_FOUND));
        Policy policy =
                policyRepository
                        .findByIdAndDeletedAtIsNull(assignment.getPolicyId())
                        .orElseThrow(
                                () -> new ApplicationException(PolicyErrorCode.POLICY_NOT_FOUND));
        return policy.getPolicyType();
    }

    /** 요청자 ID 목록 추출 */
    private Set<Long> extractRequesterIds(List<PolicyAppeal> appeals) {
        return appeals.stream().map(PolicyAppeal::getRequesterId).collect(Collectors.toSet());
    }

    /** 고객 이름 일괄 조회 */
    private Map<Long, String> loadCustomerNameMap(Set<Long> customerIds) {
        if (customerIds.isEmpty()) {
            return Map.of();
        }
        return customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));
    }

    /** 커서 페이지 크기 보정 */
    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_CURSOR_SIZE;
        }
        return Math.min(size, MAX_CURSOR_SIZE);
    }
}
