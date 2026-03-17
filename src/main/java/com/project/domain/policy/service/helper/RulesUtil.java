package com.project.domain.policy.service.helper;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.PolicyErrorCode;
import com.project.domain.policy.enums.PolicyType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RulesUtil {

    private final ObjectMapper objectMapper;

    // policy 변환
    public String toPolicyKey(PolicyType type) {
        return switch (type) {
            case MANUAL_BLOCK -> "BLOCK:ACCESS";
            case MONTHLY_LIMIT -> "LIMIT:DATA:MONTHLY";
            case TIME_BLOCK -> "BLOCK:TIME";
            default -> throw new ApplicationException(PolicyErrorCode.UNSUPPORTED_POLICY_TYPE);
        };
    }

    public String serializeMonthlyLimitRule(Long limitBytes) {
        try {
            return objectMapper.writeValueAsString(new MonthlyLimitRule(limitBytes));
        } catch (JsonProcessingException e) {
            throw new ApplicationException(PolicyErrorCode.POLICY_RULES_SERIALIZATION_FAILED);
        }
    }

    private record MonthlyLimitRule(Long limitBytes) {}
}
