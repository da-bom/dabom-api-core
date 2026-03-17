package com.project.domain.admin.model;

import java.time.Instant;
import java.util.List;

public record AdminDashboard(
        long totalFamilies,
        long activeFamilies,
        long totalUsers,
        long blockedUsers,
        long todayEvents,
        long currentTps,
        SystemHealth systemHealth,
        List<RecentBlock> recentBlocks) {

    public record SystemHealth(String redis, String kafka, String mysql) {}

    public record RecentBlock(Long familyId, Long customerId, String reason, Instant blockedAt) {}
}
