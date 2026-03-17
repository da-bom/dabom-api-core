package com.project.domain.policy.service.helper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.PolicyErrorCode;
import com.project.domain.policy.enums.PolicyType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PolicyConstraintValueNormalizer {
    public static final String LIMIT_BYTES = "limitBytes";
    public static final String START = "start";
    public static final String END = "end";
    public static final String REASON = "reason";
    public static final String BLOCKED_APPS = "blockedApps";
    private static final Pattern HHMM_PATTERN = Pattern.compile("^\\d{4}$");

    // 정책 유형과 규칙 맵을 받아서 Redis에 저장할 단일 문자열로 변환한다.
    public String normalizeValue(PolicyType type, Map<String, Object> rules) {
        return switch (type) {
            case MONTHLY_LIMIT -> normalizeMonthlyLimit(rules);
            case TIME_BLOCK -> normalizeTimeBlock(rules);
            case MANUAL_BLOCK -> normalizeManualBlock(rules);
            case APP_BLOCK -> normalizeAppBlock(rules);
        };
    }

    public static String rulesToJson(ObjectMapper objectMapper, Map<String, Object> rules) {
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (JsonProcessingException e) {
            throw new ApplicationException(PolicyErrorCode.POLICY_RULES_SERIALIZATION_FAILED);
        }
    }

    private String normalizeMonthlyLimit(Map<String, Object> rules) {
        // {"limitBytes": 123} -> "123"
        Object limitBytesObj = rules.get(LIMIT_BYTES);
        return String.valueOf(toPositiveLong(limitBytesObj, LIMIT_BYTES));
    }

    private String normalizeTimeBlock(Map<String, Object> rules) {
        // {"start":"22:00","end":"07:00"} -> "2200-0700"
        Object startObj = rules.get(START);
        Object endObj = rules.get(END);
        String start = toHhmm(startObj, START);
        String end = toHhmm(endObj, END);
        return start + "-" + end;
    }

    private String normalizeManualBlock(Map<String, Object> rules) {
        // reason 존재 여부로 수동 차단 활성화 판단 -> Redis 값은 "1"
        Object reasonObj = rules.get(REASON);
        if (reasonObj == null || reasonObj.toString().isBlank()) {
            log.warn("MANUAL_BLOCK requires reason, but received: {}", reasonObj);
            throw new ApplicationException(PolicyErrorCode.INVALID_POLICY_CONSTRAINT_VALUE);
        }
        return "1";
    }

    private String normalizeAppBlock(Map<String, Object> rules) {
        // {"blockedApps":[...]} -> "app1,app2"
        return String.join(",", normalizeAppBlockValueAsSet(rules));
    }

    public Set<String> normalizeAppBlockValueAsSet(Map<String, Object> rules) {
        Object blockedAppsObj = rules.get(BLOCKED_APPS);
        if (!(blockedAppsObj instanceof List<?> blockedAppsList)) {
            log.warn("blockedApps must be a list, but received: {}", blockedAppsObj);
            throw new ApplicationException(PolicyErrorCode.INVALID_POLICY_CONSTRAINT_VALUE);
        }

        // 앱 ID는 중복 제거, 앞뒤 공백 제거, 빈 문자열 필터링 후 LinkedHashSet으로 수집
        Set<String> apps =
                blockedAppsList.stream()
                        .map(Object::toString)
                        .map(String::trim)
                        .filter(appId -> !appId.isBlank())
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        if (apps.isEmpty()) {
            log.warn("blockedApps is empty after filtering");
            throw new ApplicationException(PolicyErrorCode.INVALID_POLICY_CONSTRAINT_VALUE);
        }
        return apps;
    }

    private long toPositiveLong(Object value, String fieldName) {
        if (value == null) {
            log.warn("Missing required field: {}", fieldName);
            throw new ApplicationException(PolicyErrorCode.INVALID_POLICY_CONSTRAINT_VALUE);
        }
        try {
            long parsed = Long.parseLong(value.toString().trim());
            if (parsed <= 0) {
                log.warn("{} must be positive, but received: {}", fieldName, parsed);
                throw new ApplicationException(PolicyErrorCode.INVALID_POLICY_CONSTRAINT_VALUE);
            }
            return parsed;
        } catch (NumberFormatException e) {
            log.warn("Invalid number for {}: {}", fieldName, value);
            throw new ApplicationException(PolicyErrorCode.INVALID_POLICY_CONSTRAINT_VALUE);
        }
    }

    private String toHhmm(Object value, String fieldName) {
        if (value == null) {
            log.warn("Missing required field: {}", fieldName);
            throw new ApplicationException(PolicyErrorCode.INVALID_POLICY_CONSTRAINT_VALUE);
        }
        String normalized = value.toString().replace(":", "").trim();
        if (!HHMM_PATTERN.matcher(normalized).matches() || !isValidHhmm(normalized)) {
            log.warn("Invalid HHMM for {}: {}", fieldName, value);
            throw new ApplicationException(PolicyErrorCode.INVALID_POLICY_CONSTRAINT_VALUE);
        }
        return normalized;
    }

    private boolean isValidHhmm(String hhmm) {
        int hh = Integer.parseInt(hhmm.substring(0, 2));
        int mm = Integer.parseInt(hhmm.substring(2, 4));
        return hh >= 0 && hh <= 23 && mm >= 0 && mm <= 59;
    }
}
