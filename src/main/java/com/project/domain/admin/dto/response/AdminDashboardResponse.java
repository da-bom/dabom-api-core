package com.project.domain.admin.dto.response;

import java.time.Instant;
import java.util.List;

public record AdminDashboardResponse(
        long totalFamilies,
        long activeFamilies,
        long totalUsers,
        long blockedUsers,
        long todayEvents,
        long currentTps,
        SystemHealthResponse systemHealth,
        List<RecentBlockResponse> recentBlocks) {

    public record SystemHealthResponse(String redis, String kafka, String mysql) {}

    public record RecentBlockResponse(
            Long familyId, Long customerId, String reason, Instant blockedAt) {}
}
