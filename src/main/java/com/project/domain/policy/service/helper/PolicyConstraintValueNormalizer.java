package com.project.domain.policy.service.helper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.project.domain.policy.enums.PolicyType;

@Component
public class PolicyConstraintValueNormalizer {
    public static final String LIMIT_BYTES = "limitBytes";
    public static final String START = "start";
    public static final String END = "end";
    public static final String REASON = "reason";
    public static final String BLOCKED_APPS = "blockedApps";
    private static final Pattern HHMM_PATTERN = Pattern.compile("^\\d{4}$");

    public String normalizeValue(PolicyType type, Map<String, Object> rules) {
        return switch (type) {
            case MONTHLY_LIMIT -> normalizeMonthlyLimit(rules);
            case TIME_BLOCK -> normalizeTimeBlock(rules);
            case MANUAL_BLOCK -> normalizeManualBlock(rules);
            case APP_BLOCK -> normalizeAppBlock(rules);
        };
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
            throw new IllegalArgumentException("MANUAL_BLOCK requires reason");
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
            throw new IllegalArgumentException("blockedApps must be a list");
        }

        Set<String> apps =
                blockedAppsList.stream()
                        .map(Object::toString)
                        .map(String::trim)
                        .filter(appId -> !appId.isBlank())
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        if (apps.isEmpty()) {
            throw new IllegalArgumentException("blockedApps is empty");
        }
        return apps;
    }

    private long toPositiveLong(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException("Missing " + fieldName);
        }
        try {
            long parsed = Long.parseLong(value.toString().trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException(fieldName + " must be positive");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number for " + fieldName, e);
        }
    }

    private String toHhmm(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException("Missing " + fieldName);
        }
        String normalized = value.toString().replace(":", "").trim();
        if (!HHMM_PATTERN.matcher(normalized).matches() || !isValidHhmm(normalized)) {
            throw new IllegalArgumentException("Invalid HHMM for " + fieldName);
        }
        return normalized;
    }

    private boolean isValidHhmm(String hhmm) {
        int hh = Integer.parseInt(hhmm.substring(0, 2));
        int mm = Integer.parseInt(hhmm.substring(2, 4));
        return hh >= 0 && hh <= 23 && mm >= 0 && mm <= 59;
    }
}
