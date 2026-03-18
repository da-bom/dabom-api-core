package com.project.domain.policy.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dabom.messaging.kafka.event.dto.notification.NotificationEventSupport;
import com.dabom.messaging.kafka.event.dto.notification.NotificationPayload;
import com.dabom.messaging.kafka.event.dto.notification.NotificationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.auth.enums.RoleType;
import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.CustomerErrorCode;
import com.project.common.exception.code.FamilyErrorCode;
import com.project.common.exception.code.PolicyErrorCode;
import com.project.domain.customer.entity.CustomerQuota;
import com.project.domain.customer.repository.CustomerQuotaRepository;
import com.project.domain.eventoutbox.service.NotificationOutboxPublisher;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.policy.dto.response.FamilyPolicyResponse;
import com.project.domain.policy.entity.PolicyAssignment;
import com.project.domain.policy.enums.PolicyType;
import com.project.domain.policy.repository.PolicyAssignmentRepository;
import com.project.domain.policy.repository.PolicyQueryRepository;
import com.project.domain.policy.service.helper.PolicyConstraintValueNormalizer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FamilyPolicyServiceImpl implements FamilyPolicyService {

    private static final String MANUAL_BLOCK_REASON = "MANUAL_BLOCK";
    private static final String QUOTA_EXCEEDED_REASON = "QUOTA_EXCEEDED";
    private static final double BYTES_PER_GIGABYTE = 1024.0 * 1024.0 * 1024.0;

    private final PolicyAssignmentRepository policyAssignmentRepository;
    private final PolicyQueryRepository policyQueryRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final PolicyConstraintValueNormalizer policyConstraintValueNormalizer;
    private final PolicyRedisService policyRedisService;
    private final CustomerQuotaRepository customerQuotaRepository;
    private final NotificationOutboxPublisher notificationOutboxPublisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public FamilyPolicyResponse getFamilyPolicyResponse(Long customerId) {
        List<FamilyPolicyResponse.FlatPolicyRow> flatList =
                policyQueryRepository.findAllFamilyPoliciesByCustomerId(customerId, currentMonth());
        return FamilyPolicyResponse.from(flatList);
    }

    @Override
    @Transactional
    public void updateMemberPolicy(
            Long targetCustomerId,
            PolicyType type,
            Map<String, Object> rules,
            Boolean isActive,
            Long actorId) {

        validateOwner(actorId);

        Long familyId = getFamilyId(actorId);
        PolicyAssignment assignment = getPolicyAssignment(familyId, targetCustomerId, type);

        Map<String, Object> effectiveRules = resolveEffectiveRules(assignment, rules);
        boolean effectiveIsActive = isActive != null ? isActive : assignment.isActive();
        String updatedRulesJson = policyConstraintValueNormalizer.rulesToJson(effectiveRules);

        if (isNoOp(assignment, updatedRulesJson, effectiveIsActive)) {
            return;
        }

        assignment.update(updatedRulesJson, effectiveIsActive, actorId);
        policyRedisService.syncToRedis(
                familyId, targetCustomerId, type, effectiveRules, effectiveIsActive);

        handlePolicyChange(
                familyId,
                targetCustomerId,
                assignment,
                type,
                effectiveRules,
                effectiveIsActive,
                actorId);
    }

    private void validateOwner(Long actorId) {
        RoleType actorRole =
                familyMemberRepository
                        .findByCustomerIdAndDeletedAtIsNull(actorId)
                        .map(com.project.domain.family.entity.FamilyMember::getRole)
                        .orElse(null);
        if (actorRole != RoleType.OWNER) {
            throw new ApplicationException(PolicyErrorCode.POLICY_OWNER_ONLY);
        }
    }

    private Long getFamilyId(Long actorId) {
        return policyQueryRepository
                .findFamilyIdByTargetCustomerId(actorId)
                .orElseThrow(() -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND));
    }

    private PolicyAssignment getPolicyAssignment(
            Long familyId, Long targetCustomerId, PolicyType type) {
        return policyAssignmentRepository
                .findByTargetAndType(familyId, targetCustomerId, type)
                .orElseThrow(
                        () ->
                                new ApplicationException(
                                        PolicyErrorCode.POLICY_ASSIGNMENT_NOT_FOUND));
    }

    private Map<String, Object> resolveEffectiveRules(
            PolicyAssignment assignment, Map<String, Object> requestedRules) {
        if (requestedRules != null) {
            return requestedRules;
        }
        return deserializeRules(assignment.getRules());
    }

    private Map<String, Object> deserializeRules(String rulesJson) {
        try {
            return objectMapper.readValue(rulesJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new ApplicationException(PolicyErrorCode.POLICY_RULES_SERIALIZATION_FAILED);
        }
    }

    private boolean isNoOp(
            PolicyAssignment assignment, String updatedRulesJson, boolean effectiveIsActive) {
        return Objects.equals(assignment.getRules(), updatedRulesJson)
                && assignment.isActive() == effectiveIsActive;
    }

    private void handlePolicyChange(
            Long familyId,
            Long targetCustomerId,
            PolicyAssignment assignment,
            PolicyType type,
            Map<String, Object> effectiveRules,
            boolean effectiveIsActive,
            Long actorId) {
        switch (type) {
            case MONTHLY_LIMIT ->
                    handleMonthlyLimitChange(
                            familyId,
                            targetCustomerId,
                            assignment,
                            effectiveRules,
                            effectiveIsActive,
                            actorId);
            case MANUAL_BLOCK ->
                    handleManualBlockChange(
                            familyId,
                            targetCustomerId,
                            assignment,
                            effectiveRules,
                            effectiveIsActive,
                            actorId);
            case TIME_BLOCK, APP_BLOCK ->
                    publishPolicyChangedNotification(
                            familyId,
                            targetCustomerId,
                            assignment,
                            type,
                            effectiveIsActive,
                            actorId);
        }
    }

    private void handleMonthlyLimitChange(
            Long familyId,
            Long targetCustomerId,
            PolicyAssignment assignment,
            Map<String, Object> effectiveRules,
            boolean effectiveIsActive,
            Long actorId) {
        CustomerQuota customerQuota = getCurrentMonthCustomerQuota(familyId, targetCustomerId);
        String normalizedLimitValue =
                policyConstraintValueNormalizer.normalizeValue(
                        PolicyType.MONTHLY_LIMIT, effectiveRules);
        Long newLimitBytes = parseMonthlyLimitBytes(normalizedLimitValue);

        customerQuota.changeMonthlyLimitBytes(newLimitBytes);
        customerQuota.refreshQuotaExceededBlock(effectiveIsActive, QUOTA_EXCEEDED_REASON);

        notificationOutboxPublisher.enqueueAndPublishAfterCommit(
                new NotificationPayload(
                        familyId,
                        targetCustomerId,
                        NotificationType.QUOTA_UPDATED,
                        NotificationEventSupport.resolveTitle(NotificationType.QUOTA_UPDATED),
                        buildQuotaUpdatedMessage(newLimitBytes),
                        buildQuotaUpdatedData(
                                assignment,
                                targetCustomerId,
                                effectiveIsActive,
                                actorId,
                                newLimitBytes)));
    }

    private void handleManualBlockChange(
            Long familyId,
            Long targetCustomerId,
            PolicyAssignment assignment,
            Map<String, Object> effectiveRules,
            boolean effectiveIsActive,
            Long actorId) {
        CustomerQuota customerQuota = getCurrentMonthCustomerQuota(familyId, targetCustomerId);

        if (effectiveIsActive) {
            customerQuota.block(MANUAL_BLOCK_REASON);
            notificationOutboxPublisher.enqueueAndPublishAfterCommit(
                    new NotificationPayload(
                            familyId,
                            targetCustomerId,
                            NotificationType.CUSTOMER_BLOCKED,
                            NotificationEventSupport.resolveTitle(
                                    NotificationType.CUSTOMER_BLOCKED),
                            buildCustomerBlockedMessage(effectiveRules),
                            buildPolicyNotificationData(
                                    assignment,
                                    PolicyType.MANUAL_BLOCK,
                                    targetCustomerId,
                                    true,
                                    actorId)));
            return;
        }

        customerQuota.tryUnblockManually(QUOTA_EXCEEDED_REASON);
        notificationOutboxPublisher.enqueueAndPublishAfterCommit(
                new NotificationPayload(
                        familyId,
                        targetCustomerId,
                        NotificationType.CUSTOMER_UNBLOCKED,
                        NotificationEventSupport.resolveTitle(NotificationType.CUSTOMER_UNBLOCKED),
                        "수동 차단 정책이 해제되었어요.",
                        buildManualUnblockedData(
                                assignment,
                                targetCustomerId,
                                actorId,
                                customerQuota.isBlocked(),
                                customerQuota.getBlockReason())));
    }

    private void publishPolicyChangedNotification(
            Long familyId,
            Long targetCustomerId,
            PolicyAssignment assignment,
            PolicyType type,
            boolean effectiveIsActive,
            Long actorId) {
        notificationOutboxPublisher.enqueueAndPublishAfterCommit(
                new NotificationPayload(
                        familyId,
                        targetCustomerId,
                        NotificationType.POLICY_CHANGED,
                        NotificationEventSupport.resolveTitle(NotificationType.POLICY_CHANGED),
                        buildPolicyChangedMessage(type),
                        buildPolicyNotificationData(
                                assignment, type, targetCustomerId, effectiveIsActive, actorId)));
    }

    private CustomerQuota getCurrentMonthCustomerQuota(Long familyId, Long customerId) {
        return customerQuotaRepository
                .findByFamilyIdAndCustomerIdAndCurrentMonthAndDeletedAtIsNull(
                        familyId, customerId, currentMonth())
                .orElseThrow(
                        () -> new ApplicationException(CustomerErrorCode.CUSTOMER_QUOTA_NOT_FOUND));
    }

    private Long parseMonthlyLimitBytes(String normalizedLimitValue) {
        if (normalizedLimitValue == null) {
            return null;
        }
        try {
            return Long.parseLong(normalizedLimitValue);
        } catch (NumberFormatException e) {
            throw new ApplicationException(PolicyErrorCode.INVALID_POLICY_CONSTRAINT_VALUE);
        }
    }

    private String buildQuotaUpdatedMessage(Long newLimitBytes) {
        if (newLimitBytes == null) {
            return "개인 데이터 사용 한도가 무제한으로 변경되었어요.";
        }
        double limitInGb = newLimitBytes / BYTES_PER_GIGABYTE;
        return "개인 데이터 사용 한도가 %.2fGB로 변경되었어요.".formatted(limitInGb);
    }

    private Map<String, Object> buildQuotaUpdatedData(
            PolicyAssignment assignment,
            Long targetCustomerId,
            boolean effectiveIsActive,
            Long actorId,
            Long newLimitBytes) {
        Map<String, Object> data =
                new LinkedHashMap<>(
                        buildPolicyNotificationData(
                                assignment,
                                PolicyType.MONTHLY_LIMIT,
                                targetCustomerId,
                                effectiveIsActive,
                                actorId));
        data.put("monthlyLimitBytes", newLimitBytes);
        return data;
    }

    private String buildCustomerBlockedMessage(Map<String, Object> effectiveRules) {
        Object reason = effectiveRules.get(PolicyConstraintValueNormalizer.REASON);
        if (reason == null || reason.toString().isBlank()) {
            return "수동 차단 정책이 적용되었어요.";
        }
        return "수동 차단 정책이 적용되었어요. 사유: " + reason;
    }

    private String buildPolicyChangedMessage(PolicyType type) {
        return switch (type) {
            case TIME_BLOCK -> "시간대 차단 정책이 변경되었어요.";
            case APP_BLOCK -> "앱 차단 정책이 변경되었어요.";
            case MONTHLY_LIMIT -> "개인 데이터 사용 한도가 변경되었어요.";
            case MANUAL_BLOCK -> "수동 차단 정책이 변경되었어요.";
        };
    }

    private Map<String, Object> buildPolicyNotificationData(
            PolicyAssignment assignment,
            PolicyType type,
            Long targetCustomerId,
            boolean effectiveIsActive,
            Long actorId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("policyAssignmentId", assignment.getId());
        data.put("policyType", type.name());
        data.put("targetCustomerId", targetCustomerId);
        data.put("isActive", effectiveIsActive);
        data.put("actorId", actorId);
        return data;
    }

    private Map<String, Object> buildManualUnblockedData(
            PolicyAssignment assignment,
            Long targetCustomerId,
            Long actorId,
            boolean stillBlocked,
            String blockReason) {
        Map<String, Object> data =
                new LinkedHashMap<>(
                        buildPolicyNotificationData(
                                assignment,
                                PolicyType.MANUAL_BLOCK,
                                targetCustomerId,
                                false,
                                actorId));
        data.put("stillBlocked", stillBlocked);
        data.put("blockReason", blockReason);
        return data;
    }

    private LocalDate currentMonth() {
        return LocalDate.now(clock).withDayOfMonth(1);
    }
}
