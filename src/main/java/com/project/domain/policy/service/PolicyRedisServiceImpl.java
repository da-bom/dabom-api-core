package com.project.domain.policy.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.PolicyErrorCode;
import com.project.common.util.LogSanitizer;
import com.project.common.util.RedisKeyGenerator;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.policy.enums.PolicyType;
import com.project.domain.policy.service.helper.PolicyConstraintValueNormalizer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyRedisServiceImpl implements PolicyRedisService {
    private final RedisTemplate<String, String> familyStringRedisTemplate;
    private final RedisKeyGenerator redisKeyGenerator;
    private final FamilyMemberRepository familyMemberRepository;
    private final PolicyConstraintValueNormalizer policyConstraintValueNormalizer;
    private final LogSanitizer logSanitizer;

    @Override
    public void syncToRedis(
            Long familyId,
            Long targetCustomerId,
            PolicyType type,
            Map<String, Object> rules,
            boolean isActive) {

        String policyKey = type.getRedisKey();

        // 정책 타입별로 Redis 저장 형식에 맞는 값으로 정규화
        NormalizedPolicyValue normalizedPolicyValue =
                resolveNormalizedPolicyValue(type, rules, isActive);

        // familyId/targetCustomerId가 모두 없으면 전체 활성 구성원에 대해 정책을 반영
        if (familyId == null && targetCustomerId == null) {
            processGlobalPolicyUpdate(type, normalizedPolicyValue);
            return;
        }

        // targetCustomerId가 있으면 해당 customer만 반영
        if (targetCustomerId != null) {
            processCustomerPolicyUpdate(familyId, targetCustomerId, type, normalizedPolicyValue);
            return;
        }

        // targetCustomerId가 없으면 family 전체(active customer)에게 반영
        List<FamilyMemberRepository.FamilyMemberTargetProjection> customers =
                familyMemberRepository.findAllActiveTargetsByFamilyId(familyId);

        for (FamilyMemberRepository.FamilyMemberTargetProjection customer : customers) {
            processCustomerPolicyUpdate(
                    customer.getFamilyId(), customer.getCustomerId(), type, normalizedPolicyValue);
        }

        log.info(
                "Processed family-wide constraint. familyId={}, memberCount={}," + " field={}",
                familyId,
                customers.size(),
                logSanitizer.sanitize(policyKey));
    }

    // 전체 업데이트는 활성 구성원 조회 후, 각 구성원별 업데이트 로직을 재사용해서 처리
    private void processGlobalPolicyUpdate(
            PolicyType type, NormalizedPolicyValue normalizedPolicyValue) {
        List<FamilyMemberRepository.FamilyMemberTargetProjection> members =
                familyMemberRepository.findAllActiveTargets();

        for (FamilyMemberRepository.FamilyMemberTargetProjection member : members) {
            processCustomerPolicyUpdate(
                    member.getFamilyId(), member.getCustomerId(), type, normalizedPolicyValue);
        }

        log.info(
                "Processed global constraint. memberCount={}, field={}",
                members.size(),
                logSanitizer.sanitize(type.getRedisKey()));
    }

    private NormalizedPolicyValue resolveNormalizedPolicyValue(
            PolicyType type, Map<String, Object> rules, boolean isActive) {
        // 비활성화 정책이면 삭제(HDEL)로 처리되도록 null 값을 전달
        if (!isActive) {
            return new NormalizedPolicyValue(null, Set.of());
        }

        // APP_BLOCK 정책은 개별 앱 ID를 필드로 관리하기 때문에, 전체 차단 앱 목록과 Redis 간 동기화가 필요함
        if (type == PolicyType.APP_BLOCK) {
            Set<String> normalizedBlockedApps =
                    policyConstraintValueNormalizer.normalizeAppBlockValueAsSet(rules);
            return new NormalizedPolicyValue(
                    String.join(",", normalizedBlockedApps), normalizedBlockedApps);
        }

        // 그 외 정책은 단일 값으로 관리하므로, 정규화된 문자열 값을 전달
        return new NormalizedPolicyValue(
                policyConstraintValueNormalizer.normalizeValue(type, rules), Set.of());
    }

    private void processCustomerPolicyUpdate(
            Long familyId,
            Long customerId,
            PolicyType type,
            NormalizedPolicyValue normalizedPolicyValue) {
        // 업데이트는 기존 캐시 갱신만 담당하고, 캐시 미스는 스킵
        if (!hasConstraintsKey(familyId, customerId)) {
            return;
        }

        if (type == PolicyType.APP_BLOCK) {
            syncBlockedAppsToCustomer(
                    familyId, customerId, type, normalizedPolicyValue.normalizedBlockedApps());
            return;
        }

        applyConstraintToCustomer(
                familyId,
                customerId,
                type.getRedisKey(),
                normalizedPolicyValue.normalizedNewValue());
    }

    private boolean hasConstraintsKey(Long familyId, Long customerId) {
        String constraintsKey =
                redisKeyGenerator.generateFamilyCustomerConstraintsKey(familyId, customerId);
        Boolean exists = familyStringRedisTemplate.hasKey(constraintsKey);
        return Boolean.TRUE.equals(exists);
    }

    // APP_BLOCK 정책은 개별 앱 ID를 필드로 관리하기 때문에, 전체 차단 앱 목록과 Redis 간 동기화가 필요함
    private void syncBlockedAppsToCustomer(
            Long familyId, Long customerId, PolicyType type, Set<String> desiredBlockedApps) {
        // Redis에서 현재 차단 앱 목록 로드
        String constraintsKey =
                redisKeyGenerator.generateFamilyCustomerConstraintsKey(familyId, customerId);
        String appFieldPrefix = type.getRedisKey() + ":";
        Set<String> currentBlockedApps = loadBlockedApps(constraintsKey, appFieldPrefix);

        // 현재 차단 앱과 원하는 차단 앱을 비교해서, 삭제할 앱과 추가할 앱을 계산
        Set<String> appsToDelete = new LinkedHashSet<>(currentBlockedApps);
        appsToDelete.removeAll(desiredBlockedApps);

        Set<String> appsToAdd = new LinkedHashSet<>(desiredBlockedApps);
        appsToAdd.removeAll(currentBlockedApps);

        if (appsToDelete.isEmpty() && appsToAdd.isEmpty()) {
            return;
        }

        // 삭제와 추가를 하나의 Redis 트랜잭션으로 묶어 부분 반영을 방지한다.
        try {
            List<Object> transactionResults =
                    familyStringRedisTemplate.execute(
                            new SessionCallback<>() {
                                @Override
                                @SuppressWarnings("unchecked")
                                public List<Object> execute(RedisOperations operations) {
                                    operations.multi();

                                    if (!appsToDelete.isEmpty()) {
                                        Object[] deleteFields =
                                                appsToDelete.stream()
                                                        .map(appId -> appFieldPrefix + appId)
                                                        .toArray();
                                        operations
                                                .opsForHash()
                                                .delete(constraintsKey, deleteFields);
                                    }

                                    if (!appsToAdd.isEmpty()) {
                                        Map<String, String> addEntries = new LinkedHashMap<>();
                                        for (String appId : appsToAdd) {
                                            addEntries.put(appFieldPrefix + appId, "1");
                                        }
                                        operations.opsForHash().putAll(constraintsKey, addEntries);
                                    }

                                    return operations.exec();
                                }
                            });

            if (transactionResults == null) {
                throw new ApplicationException(PolicyErrorCode.POLICY_REDIS_SYNC_FAILED);
            }
        } catch (Exception e) {
            log.error(
                    "Failed to sync blocked apps to Redis. familyId={}, customerId={}",
                    familyId,
                    customerId,
                    e);
            throw new ApplicationException(PolicyErrorCode.POLICY_REDIS_SYNC_FAILED);
        }
    }

    // Redis에서 현재 차단 앱 목록을 로드하는 헬퍼 메서드
    private Set<String> loadBlockedApps(String constraintsKey, String appFieldPrefix) {
        Set<Object> fields = familyStringRedisTemplate.opsForHash().keys(constraintsKey);
        if (fields.isEmpty()) {
            return Set.of();
        }

        return fields.stream()
                .map(String::valueOf)
                .filter(field -> field.startsWith(appFieldPrefix))
                .map(field -> field.substring(appFieldPrefix.length()))
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
                            + " customerId={}, field={}, value={}",
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

    private record NormalizedPolicyValue(
            String normalizedNewValue, Set<String> normalizedBlockedApps) {}
}
