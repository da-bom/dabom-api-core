package com.project.family.core;

import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.GlobalErrorCode;

/** 가족 그룹 도메인 정책/규칙 (Policy) */
public class FamilyRule {

    private static final long MIN_QUOTA_BYTES = 1024 * 1024 * 100L; // 최소 100MB

    public static void validateQuota(Long quotaBytes) {
        if (quotaBytes == null || quotaBytes < MIN_QUOTA_BYTES) {
            // TODO: 추후 FamilyErrorCode 추가 시 변경
            throw new ApplicationException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
