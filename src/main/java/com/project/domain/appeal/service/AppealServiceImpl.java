package com.project.domain.appeal.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.appeal.dto.request.AppealCommentRequest;
import com.project.domain.appeal.dto.request.AppealCreateRequest;
import com.project.domain.appeal.dto.request.AppealRespondRequest;
import com.project.domain.appeal.dto.request.EmergencyQuotaRequest;
import com.project.domain.appeal.entity.PolicyAppeal;
import com.project.domain.appeal.entity.PolicyAppealComment;
import com.project.domain.appeal.enums.AppealStatus;
import com.project.domain.appeal.enums.AppealType;
import com.project.domain.appeal.model.AppealCancelResult;
import com.project.domain.appeal.model.AppealCommentResult;
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
    private static final long EMERGENCY_ADDITIONAL_BYTES = 314_572_800L;

    private final Clock clock;
    private final PolicyAppealRepository policyAppealRepository;
    private final PolicyAppealCommentRepository policyAppealCommentRepository;
    private final CustomerRepository customerRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final PolicyAssignmentRepository policyAssignmentRepository;
    private final PolicyRepository policyRepository;
    private final CustomerQuotaRepository customerQuotaRepository;
    private final ObjectMapper objectMapper;

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
        Map<Long, PolicyType> policyTypeMap = loadPolicyTypeMap(page);

        // 4. 현재 페이지 엔티티를 응답 모델로 변환해 목록 결과를 반환한다.
        return new AppealListResult(
                page.stream()
                        .map(appeal -> toAppealSummary(appeal, customerNameMap, policyTypeMap))
                        .toList(),
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

        // 2. MEMBER에 대해서는 반드시 본인 소유 appeal인지까지 확인한다.
        if (!auth.isOwner() && !appeal.getRequesterId().equals(auth.customerId())) {
            throw new ApplicationException(AppealErrorCode.APPEAL_FORBIDDEN);
        }

        // 3. 댓글 size를 보정하고 커서 기반으로 댓글을 조회한다.
        int pageSize = normalizeSize(size);
        List<PolicyAppealComment> comments =
                policyAppealCommentRepository.findByAppealIdAndIdLessThanOrderByIdDesc(
                        appealId, cursor, PageRequest.of(0, pageSize + 1));

        // 4. pageSize + 1 조회 결과로 댓글 nextCursor와 hasNext를 계산한다.
        boolean hasNext = comments.size() > pageSize;
        List<PolicyAppealComment> page = hasNext ? comments.subList(0, pageSize) : comments;
        String nextCursor = hasNext ? String.valueOf(page.getLast().getId()) : null;

        // 5. 요청자, 처리자, 댓글 작성자 이름과 정책 타입을 조회해 상세 응답으로 변환한다.
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

        // 2. 정책 할당이 존재하고 같은 가족 소속인지, 본인 대상인지 확인한다.
        PolicyAssignment policyAssignment =
                policyAssignmentRepository
                        .findByIdAndDeletedAtIsNull(request.policyAssignmentId())
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                PolicyErrorCode.POLICY_ASSIGNMENT_NOT_FOUND));
        if (!policyAssignment.getFamilyId().equals(auth.familyId())
                || !policyAssignment.getTargetCustomerId().equals(auth.customerId())) {
            throw new ApplicationException(AppealErrorCode.APPEAL_FORBIDDEN);
        }

        // 3. 같은 정책에 대해 같은 요청자의 진행 중 이의제기가 이미 있는지 확인한다.
        validateNoPendingAppeal(policyAssignment.getId(), auth.customerId());

        // 4. NORMAL/PENDING 상태의 이의제기를 저장한다.
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

        // 5. 생성 결과를 응답 모델로 반환한다.
        return new AppealCreateResult(
                appeal.getId(),
                appeal.getPolicyAssignmentId(),
                appeal.getStatus(),
                appeal.getDesiredRules(),
                appeal.getCreatedAt());
    }

    /** 이의제기 승인/거절 */
    @Override
    @Transactional
    public AppealRespondResult respondAppeal(
            AuthContext auth, Long appealId, AppealRespondRequest request) {
        // 1. OWNER만 요청을 승인/거절할 수 있다.
        validateOwnerOnly(auth);

        // 2. 이의제기 존재 여부와 같은 가족 접근 가능 여부를 검증한다.
        PolicyAppeal appeal = findAppealOrThrow(appealId);
        validateFamilyAccess(auth.familyId(), appeal);

        // 3. 아직 처리 가능한 PENDING 상태인지 확인한다.
        if (!appeal.isPending()) {
            throw new ApplicationException(AppealErrorCode.APPEAL_ALREADY_RESOLVED);
        }

        // 4. action 값을 승인/거절 중 하나로 파싱한다.
        AppealStatus action = parseRespondAction(request.action());
        LocalDateTime now = LocalDateTime.now(clock);

        // 5. APPROVED면 승인 처리하고 desiredRules가 있으면 정책 규칙에도 반영한다.
        if (AppealStatus.APPROVED.equals(action)) {
            appeal.approve(auth.customerId(), now);
            applyDesiredRulesIfPresent(appeal, auth.customerId());
        } else {
            // 6. REJECTED면 거절 사유를 검증한 뒤 거절 처리한다.
            if (request.rejectReason() == null || request.rejectReason().isBlank()) {
                throw new ApplicationException(AppealErrorCode.APPEAL_REJECT_REASON_REQUIRED);
            }
            appeal.reject(auth.customerId(), request.rejectReason(), now);
        }

        return new AppealRespondResult(
                appeal.getId(),
                appeal.getStatus(),
                appeal.getRejectReason(),
                appeal.getResolvedById(),
                appeal.getResolvedAt());
    }

    /** 이의제기 댓글 작성 */
    @Override
    @Transactional
    public AppealCommentResult createComment(
            AuthContext auth, Long appealId, AppealCommentRequest request) {
        // 1. 이의제기 존재 여부와 같은 가족 접근 가능 여부를 검증한다.
        PolicyAppeal appeal = findAppealOrThrow(appealId);
        validateFamilyAccess(auth.familyId(), appeal);

        // 2. OWNER와 MEMBER 모두 댓글 작성 가능하되, 가족 소속 구성원만 허용한다.
        validateFamilyMemberExists(auth.customerId(), auth.familyId());

        // 3. 댓글을 저장하고 응답 모델로 반환한다.
        PolicyAppealComment comment =
                policyAppealCommentRepository.save(
                        PolicyAppealComment.builder()
                                .appealId(appealId)
                                .authorId(auth.customerId())
                                .comment(request.comment())
                                .build());

        // 4. 댓글 작성자 이름을 추가한다.
        return new AppealCommentResult(
                comment.getId(),
                comment.getAppealId(),
                comment.getAuthorId(),
                auth.authorName() == null || auth.authorName().isBlank()
                        ? UNKNOWN_NAME
                        : auth.authorName(),
                comment.getComment(),
                comment.getCreatedAt());
    }

    /** 이의제기 취소 */
    @Override
    @Transactional
    public AppealCancelResult cancelAppeal(AuthContext auth, Long appealId) {
        // 1. MEMBER만 본인 이의제기를 취소할 수 있다.
        validateMemberOnly(auth);

        // 2. 이의제기 존재 여부를 확인한다.
        PolicyAppeal appeal = findAppealOrThrow(appealId);

        // 3. 본인 생성 건인지 확인한다.
        if (!appeal.getRequesterId().equals(auth.customerId())) {
            throw new ApplicationException(AppealErrorCode.APPEAL_CANCEL_FORBIDDEN);
        }

        // 4. EMERGENCY 타입은 취소할 수 없다.
        if (appeal.isEmergency()) {
            throw new ApplicationException(AppealErrorCode.APPEAL_EMERGENCY_CANCEL_NOT_ALLOWED);
        }

        // 5. 아직 처리 전인 PENDING 상태만 취소할 수 있다.
        if (!appeal.isPending()) {
            throw new ApplicationException(AppealErrorCode.APPEAL_NOT_CANCELLABLE);
        }

        // 6. 취소 상태와 시각을 기록한다.
        appeal.cancel(LocalDateTime.now(clock));
        return new AppealCancelResult(appeal.getId(), appeal.getStatus(), appeal.getCancelledAt());
    }

    /** 긴급 쿼터 요청 */
    @Override
    @Transactional
    public EmergencyQuotaResult requestEmergencyQuota(
            AuthContext auth, EmergencyQuotaRequest request) {
        // 1. MEMBER 역할을 검증한다.
        validateMemberOnly(auth);

        // 2. 당월 고객 쿼터를 조회하고 무제한 사용자 여부를 확인한다.
        LocalDate currentMonth = LocalDate.now(clock).withDayOfMonth(1);
        CustomerQuota customerQuota =
                customerQuotaRepository
                        .findByFamilyIdAndCustomerIdAndCurrentMonthAndDeletedAtIsNull(
                                auth.familyId(), auth.customerId(), currentMonth)
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                CustomerErrorCode.CUSTOMER_NOT_FOUND));

        if (customerQuota.getMonthlyLimitBytes() == null) {
            throw new ApplicationException(AppealErrorCode.APPEAL_EMERGENCY_UNLIMITED);
        }

        // 3. DB unique 제약으로 같은 사용자의 당월 긴급 승인을 단일화한다.
        PolicyAppeal appeal;
        try {
            appeal =
                    policyAppealRepository.saveAndFlush(
                            PolicyAppeal.builder()
                                    .type(AppealType.EMERGENCY)
                                    .requesterId(auth.customerId())
                                    .requestReason(request.requestReason())
                                    .desiredRules(
                                            Map.of("additionalBytes", EMERGENCY_ADDITIONAL_BYTES))
                                    .status(AppealStatus.APPROVED)
                                    .emergencyGrantMonth(currentMonth)
                                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new ApplicationException(AppealErrorCode.APPEAL_EMERGENCY_MONTHLY_LIMIT);
        }

        // 4. 승인 저장이 확정되면 월 한도를 즉시 증가시킨다.
        customerQuota.addMonthlyLimitBytes(EMERGENCY_ADDITIONAL_BYTES);

        // 5. MONTHLY_LIMIT 정책 할당이 있으면 rules.limitBytes도 동기화한다.
        syncMonthlyLimitPolicyAssignment(
                auth.familyId(), auth.customerId(), customerQuota.getMonthlyLimitBytes());

        // 6. 긴급 쿼터 결과를 응답 모델로 반환한다.
        return new EmergencyQuotaResult(
                appeal.getId(),
                appeal.getType(),
                appeal.getStatus(),
                EMERGENCY_ADDITIONAL_BYTES,
                customerQuota.getMonthlyLimitBytes(),
                appeal.getRequestReason(),
                appeal.getCreatedAt());
    }

    /** 이의제기 요약 모델 변환 */
    private AppealListResult.AppealSummary toAppealSummary(
            PolicyAppeal appeal,
            Map<Long, String> customerNameMap,
            Map<Long, PolicyType> policyTypeMap) {
        return new AppealListResult.AppealSummary(
                appeal.getId(),
                appeal.getType(),
                appeal.getPolicyAssignmentId(),
                appeal.getPolicyAssignmentId() == null
                        ? null
                        : policyTypeMap.get(appeal.getPolicyAssignmentId()),
                appeal.getRequesterId(),
                customerNameMap.getOrDefault(appeal.getRequesterId(), UNKNOWN_NAME),
                appeal.getRequestReason(),
                appeal.getDesiredRules(),
                appeal.getStatus(),
                appeal.getCreatedAt());
    }

    private Map<Long, PolicyType> loadPolicyTypeMap(List<PolicyAppeal> appeals) {
        Set<Long> assignmentIds =
                appeals.stream()
                        .map(PolicyAppeal::getPolicyAssignmentId)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toSet());
        if (assignmentIds.isEmpty()) {
            return Map.of();
        }

        List<PolicyAssignment> assignments =
                policyAssignmentRepository.findAllByIdInAndDeletedAtIsNull(assignmentIds);
        Set<Long> policyIds =
                assignments.stream()
                        .map(PolicyAssignment::getPolicyId)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toSet());
        if (policyIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, PolicyType> policyTypeByPolicyId =
                policyRepository.findAllByIdInAndDeletedAtIsNull(policyIds).stream()
                        .collect(Collectors.toMap(Policy::getId, Policy::getPolicyType));

        Map<Long, PolicyType> policyTypeMap = new HashMap<>();
        for (PolicyAssignment assignment : assignments) {
            PolicyType policyType = policyTypeByPolicyId.get(assignment.getPolicyId());
            if (policyType != null) {
                policyTypeMap.put(assignment.getId(), policyType);
            }
        }
        return policyTypeMap;
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

    /** 진행 중 이의제기 중복 여부 검증 */
    private void validateNoPendingAppeal(Long policyAssignmentId, Long requesterId) {
        boolean alreadyPending =
                policyAppealRepository
                        .existsByPolicyAssignmentIdAndRequesterIdAndTypeAndStatusAndDeletedAtIsNull(
                                policyAssignmentId,
                                requesterId,
                                AppealType.NORMAL,
                                AppealStatus.PENDING);
        if (alreadyPending) {
            throw new ApplicationException(AppealErrorCode.APPEAL_ALREADY_PENDING);
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

    private void applyDesiredRulesIfPresent(PolicyAppeal appeal, Long actorId) {
        if (appeal.getDesiredRules() == null || appeal.getDesiredRules().isEmpty()) {
            return;
        }
        if (appeal.getPolicyAssignmentId() == null) {
            return;
        }
        PolicyAssignment assignment =
                policyAssignmentRepository
                        .findByIdAndDeletedAtIsNull(appeal.getPolicyAssignmentId())
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                PolicyErrorCode.POLICY_ASSIGNMENT_NOT_FOUND));
        try {
            assignment.update(
                    objectMapper.writeValueAsString(appeal.getDesiredRules()), null, actorId);
        } catch (JsonProcessingException e) {
            throw new ApplicationException(AppealErrorCode.APPEAL_INVALID_DESIRED_RULES);
        }
    }

    /** MONTHLY_LIMIT 정책 할당의 rules.limitBytes를 갱신한다. */
    private void syncMonthlyLimitPolicyAssignment(
            Long familyId, Long customerId, Long newLimitBytes) {
        policyAssignmentRepository
                .findByTargetAndType(familyId, customerId, PolicyType.MONTHLY_LIMIT)
                .ifPresent(
                        assignment -> {
                            try {
                                Map<String, Object> rules =
                                        objectMapper.readValue(
                                                assignment.getRules(),
                                                objectMapper
                                                        .getTypeFactory()
                                                        .constructMapType(
                                                                Map.class,
                                                                String.class,
                                                                Object.class));
                                rules.put("limitBytes", newLimitBytes);
                                assignment.update(
                                        objectMapper.writeValueAsString(rules), null, null);
                            } catch (JsonProcessingException e) {
                                throw new ApplicationException(
                                        AppealErrorCode.APPEAL_INVALID_DESIRED_RULES);
                            }
                        });
    }

    private void validateOwnerOnly(AuthContext auth) {
        if (!auth.isOwner()) {
            throw new ApplicationException(AppealErrorCode.APPEAL_FORBIDDEN);
        }
    }

    private void validateFamilyMemberExists(Long customerId, Long familyId) {
        FamilyMember member =
                familyMemberRepository
                        .findByCustomerId(customerId)
                        .orElseThrow(
                                () -> new ApplicationException(AppealErrorCode.APPEAL_FORBIDDEN));
        if (!member.getFamilyId().equals(familyId)) {
            throw new ApplicationException(AppealErrorCode.APPEAL_FORBIDDEN);
        }
    }

    private AppealStatus parseRespondAction(String action) {
        if (action == null || action.isBlank()) {
            throw new ApplicationException(AppealErrorCode.APPEAL_INVALID_ACTION);
        }
        try {
            AppealStatus parsed = AppealStatus.valueOf(action.toUpperCase());
            if (AppealStatus.APPROVED.equals(parsed) || AppealStatus.REJECTED.equals(parsed)) {
                return parsed;
            }
            throw new ApplicationException(AppealErrorCode.APPEAL_INVALID_ACTION);
        } catch (IllegalArgumentException e) {
            throw new ApplicationException(AppealErrorCode.APPEAL_INVALID_ACTION);
        }
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
