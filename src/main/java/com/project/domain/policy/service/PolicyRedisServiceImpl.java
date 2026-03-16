package com.project.domain.policy.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.PolicyErrorCode;
import com.project.common.util.LogSanitizer;
import com.project.common.util.RedisKeyGenerator;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.policy.enums.PolicyType;
import com.project.domain.policy.service.helper.PolicyConstraintValueNormalizer;
import com.project.domain.policy.service.helper.RulesUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyRedisServiceImpl implements PolicyRedisService {
    private static final String VALUE_LOG_SUFFIX = ", value={}";
    private static final String SKIP_REASON_MISSING_CONSTRAINTS_KEY = "MISSING_CONSTRAINTS_KEY";
    public static final String BLOCK_APP = "BLOCK:APP";
    public static final String BLOCK_APP_PREFIX = "BLOCK:APP:";

    private final RedisTemplate<String, String> familyStringRedisTemplate;
    private final RedisKeyGenerator redisKeyGenerator;
    private final FamilyMemberRepository familyMemberRepository;
    private final PolicyConstraintValueNormalizer policyConstraintValueNormalizer;
    private final LogSanitizer logSanitizer;
    private final RulesUtil rulesUtil;

    @Override
    public void syncToRedis(
            Long familyId,
            Long targetCustomerId,
            PolicyType type,
            Map<String, Object> rules,
            boolean isActive) {

        String policyKey = rulesUtil.toPolicyKey(type);

        // 정책 타입별로 Redis 저장 형식에 맞는 값으로 정규화
        NormalizedPolicyValue normalizedPolicyValue =
                resolveNormalizedPolicyValue(type, policyKey, rules, isActive);

        // familyId/targetCustomerId가 모두 없으면 전체 활성 구성원에 대해 정책을 반영
        if (familyId == null && targetCustomerId == null) {
            processGlobalPolicyUpdate(policyKey, normalizedPolicyValue);
            return;
        }

        // targetCustomerId가 있으면 해당 customer만 반영
        if (targetCustomerId != null) {
            processCustomerPolicyUpdate(
                    familyId, targetCustomerId, policyKey, normalizedPolicyValue);
            return;
        }

        // targetCustomerId가 없으면 family 전체(active customer)에게 반영
        List<FamilyMemberRepository.FamilyMemberTargetProjection> customers =
                familyMemberRepository.findAllActiveTargetsByFamilyId(familyId);
        int appliedCount = 0;
        int skippedCount = 0;

        for (FamilyMemberRepository.FamilyMemberTargetProjection customer : customers) {
            boolean applied =
                    processCustomerPolicyUpdate(
                            customer.getFamilyId(),
                            customer.getCustomerId(),
                            policyKey,
                            normalizedPolicyValue);
            if (applied) {
                appliedCount++;
            } else {
                skippedCount++;
            }
        }

        log.info(
                "Processed family-wide constraint. familyId={}, appliedCount={},"
                        + " skippedCount={}, field={}"
                        + VALUE_LOG_SUFFIX,
                familyId,
                appliedCount,
                skippedCount,
                logSanitizer.sanitize(policyKey),
                logSanitizer.sanitize(normalizedPolicyValue.normalizedNewValue()));
    }

    private void processGlobalPolicyUpdate(
            String policyKey, NormalizedPolicyValue normalizedPolicyValue) {
        List<FamilyMemberRepository.FamilyMemberTargetProjection> members =
                familyMemberRepository.findAllActiveTargets();
        AtomicInteger appliedCount = new AtomicInteger(0);
        AtomicInteger skippedCount = new AtomicInteger(0);

        members.parallelStream()
                .forEach(
                        member -> {
                            boolean applied =
                                    processCustomerPolicyUpdate(
                                            member.getFamilyId(),
                                            member.getCustomerId(),
                                            policyKey,
                                            normalizedPolicyValue);
                            if (applied) {
                                appliedCount.incrementAndGet();
                            } else {
                                skippedCount.incrementAndGet();
                            }
                        });

        log.info(
                "Processed global constraint. appliedCount={}, skippedCount={},"
                        + " field={}"
                        + VALUE_LOG_SUFFIX,
                appliedCount,
                skippedCount,
                logSanitizer.sanitize(policyKey),
                logSanitizer.sanitize(normalizedPolicyValue.normalizedNewValue()));
    }

    private NormalizedPolicyValue resolveNormalizedPolicyValue(
            PolicyType type, String policyKey, Map<String, Object> rules, boolean isActive) {
        // 비활성화 정책이면 삭제(HDEL)로 처리되도록 null 값을 전달
        if (!isActive) {
            return new NormalizedPolicyValue(null, Set.of());
        }

        if (type == PolicyType.APP_BLOCK) {
            Set<String> normalizedBlockedApps =
                    policyConstraintValueNormalizer.normalizeAppBlockValueAsSet(rules);
            return new NormalizedPolicyValue(
                    String.join(",", normalizedBlockedApps), normalizedBlockedApps);
        }

        return new NormalizedPolicyValue(
                policyConstraintValueNormalizer.normalizeValue(type, rules), Set.of());
    }

    private boolean processCustomerPolicyUpdate(
            Long familyId,
            Long customerId,
            String policyKey,
            NormalizedPolicyValue normalizedPolicyValue) {
        // 업데이트는 기존 캐시 갱신만 담당하고, 캐시 미스는 스킵
        if (!hasConstraintsKey(familyId, customerId)) {
            logResult(
                    familyId,
                    customerId,
                    policyKey,
                    normalizedPolicyValue.normalizedNewValue(),
                    SKIP_REASON_MISSING_CONSTRAINTS_KEY);
            return false;
        }

        if (BLOCK_APP.equals(policyKey)) {
            boolean changed =
                    syncBlockedAppsToCustomer(
                            familyId, customerId, normalizedPolicyValue.normalizedBlockedApps());
            logResult(
                    familyId,
                    customerId,
                    policyKey,
                    normalizedPolicyValue.normalizedNewValue(),
                    changed ? "APPLIED" : "NO_CHANGES");
            return changed;
        }

        applyConstraintToCustomer(
                familyId, customerId, policyKey, normalizedPolicyValue.normalizedNewValue());
        logResult(
                familyId,
                customerId,
                policyKey,
                normalizedPolicyValue.normalizedNewValue(),
                "APPLIED");
        return true;
    }

    private boolean hasConstraintsKey(Long familyId, Long customerId) {
        String constraintsKey =
                redisKeyGenerator.generateFamilyCustomerConstraintsKey(familyId, customerId);
        Boolean exists = familyStringRedisTemplate.hasKey(constraintsKey);
        return Boolean.TRUE.equals(exists);
    }

    private boolean syncBlockedAppsToCustomer(
            Long familyId, Long customerId, Set<String> desiredBlockedApps) {
        String constraintsKey =
                redisKeyGenerator.generateFamilyCustomerConstraintsKey(familyId, customerId);
        Set<String> currentBlockedApps = loadBlockedApps(constraintsKey);

        Set<String> appsToDelete = new LinkedHashSet<>(currentBlockedApps);
        appsToDelete.removeAll(desiredBlockedApps);

        Set<String> appsToAdd = new LinkedHashSet<>(desiredBlockedApps);
        appsToAdd.removeAll(currentBlockedApps);

        if (appsToDelete.isEmpty() && appsToAdd.isEmpty()) {
            return false;
        }

        for (String appId : appsToDelete) {
            String appField = BLOCK_APP_PREFIX + appId;
            deleteConstraint(familyId, customerId, appField);
        }

        for (String appId : appsToAdd) {
            String appField = BLOCK_APP_PREFIX + appId;
            setConstraint(familyId, customerId, appField, "1");
        }

        return true;
    }

    private Set<String> loadBlockedApps(String constraintsKey) {
        Set<Object> fields = familyStringRedisTemplate.opsForHash().keys(constraintsKey);
        if (fields.isEmpty()) {
            return Set.of();
        }

        return fields.stream()
                .map(String::valueOf)
                .filter(field -> field.startsWith(BLOCK_APP_PREFIX))
                .map(field -> field.substring(BLOCK_APP_PREFIX.length()))
                .filter(appId -> !appId.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void setConstraint(Long familyId, Long customerId, String policyKey, String value) {
        String key = redisKeyGenerator.generateFamilyCustomerConstraintsKey(familyId, customerId);
        try {
            familyStringRedisTemplate.opsForHash().put(key, policyKey, value);
        } catch (Exception e) {
            log.error(
                    "Failed to set policy constraint to Redis. familyId={},"
                            + " customerId={}, field={}"
                            + VALUE_LOG_SUFFIX,
                    familyId,
                    customerId,
                    logSanitizer.sanitize(policyKey),
                    logSanitizer.sanitize(value),
                    e);
            throw new ApplicationException(PolicyErrorCode.POLICY_REDIS_SYNC_FAILED);
        }
    }

    private void deleteConstraint(Long familyId, Long customerId, String policyKey) {
        String key = redisKeyGenerator.generateFamilyCustomerConstraintsKey(familyId, customerId);
        try {
            familyStringRedisTemplate.opsForHash().delete(key, policyKey);
        } catch (Exception e) {
            log.error(
                    "Failed to delete policy constraint from Redis. familyId={},"
                            + " customerId={}, field={}",
                    familyId,
                    customerId,
                    logSanitizer.sanitize(policyKey),
                    e);
            throw new ApplicationException(PolicyErrorCode.POLICY_REDIS_SYNC_FAILED);
        }
    }

    private void applyConstraintToCustomer(
            Long familyId, Long customerId, String policyKey, String newValue) {
        if (newValue == null || newValue.isBlank()) {
            deleteConstraint(familyId, customerId, policyKey);
        } else {
            setConstraint(familyId, customerId, policyKey, newValue);
        }
    }

    private void logResult(
            Long familyId, Long customerId, String policyKey, String newValue, String result) {
        if ("APPLIED".equals(result)) {
            log.info(
                    "Updated customer constraint. familyId={}, customerId={}, field={}"
                            + VALUE_LOG_SUFFIX,
                    familyId,
                    customerId,
                    logSanitizer.sanitize(policyKey),
                    logSanitizer.sanitize(newValue));
            return;
        }

        log.info(
                "Skipped customer constraint update. familyId={}, customerId={},"
                        + " field={}, reason={}",
                familyId,
                customerId,
                logSanitizer.sanitize(policyKey),
                logSanitizer.sanitize(result));
    }

    private record NormalizedPolicyValue(
            String normalizedNewValue, Set<String> normalizedBlockedApps) {}
}
