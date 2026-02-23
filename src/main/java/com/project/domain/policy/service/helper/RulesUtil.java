package com.project.domain.policy.service.helper;

import org.springframework.stereotype.Component;

import com.project.domain.policy.enums.PolicyType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RulesUtil {
    // policy 변환
    public String toPolicyKey(PolicyType type) {
        return switch (type) {
            case MANUAL_BLOCK -> "BLOCK:ACCESS";
            case MONTHLY_LIMIT -> "LIMIT:DATA:MONTHLY";
            case TIME_BLOCK -> "BLOCK:TIME";
            default -> throw new IllegalArgumentException("지원하지 않는 정책 타입입니다: " + type);
        };
    }
}
