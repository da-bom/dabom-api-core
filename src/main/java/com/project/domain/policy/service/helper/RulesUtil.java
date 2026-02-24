package com.project.domain.policy.service.helper;

import org.springframework.stereotype.Component;

import com.project.domain.policy.enums.PolicyType;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.PolicyErrorCode;

@Component
public class RulesUtil {
    // policy 변환
    public String toPolicyKey(PolicyType type) {
        return switch (type) {
            case MANUAL_BLOCK -> "BLOCK:ACCESS";
            case MONTHLY_LIMIT -> "LIMIT:DATA:MONTHLY";
            case TIME_BLOCK -> "BLOCK:TIME";
            default -> throw new ApplicationException(PolicyErrorCode.UNSUPPORTED_POLICY_TYPE);
        };
    }
}
