package com.project.domain.family.util;

public final class FamilyUsageCalculator {

    private FamilyUsageCalculator() {}

    public static double calculateUsedPercent(Long usedBytes, Long totalQuotaBytes) {
        if (usedBytes == null || totalQuotaBytes == null || totalQuotaBytes == 0L) {
            return 0.0;
        }
        return (double) usedBytes / totalQuotaBytes * 100.0;
    }
}
