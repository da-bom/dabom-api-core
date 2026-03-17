package com.project.domain.admin.dto.response;

import java.time.Instant;
import java.util.List;

import com.project.domain.admin.model.AdminDashboard;

public record AdminDashboardResponse(
        long totalFamilies,
        long activeFamilies,
        long totalUsers,
        long blockedUsers,
        long todayEvents,
        long currentTps,
        SystemHealthResponse systemHealth,
        List<RecentBlockResponse> recentBlocks) {

    public static AdminDashboardResponse from(AdminDashboard dashboard) {
        return new AdminDashboardResponse(
                dashboard.totalFamilies(),
                dashboard.activeFamilies(),
                dashboard.totalUsers(),
                dashboard.blockedUsers(),
                dashboard.todayEvents(),
                dashboard.currentTps(),
                SystemHealthResponse.from(dashboard.systemHealth()),
                dashboard.recentBlocks().stream().map(RecentBlockResponse::from).toList());
    }

    public record SystemHealthResponse(String redis, String kafka, String mysql) {

        public static SystemHealthResponse from(AdminDashboard.SystemHealth health) {
            return new SystemHealthResponse(health.redis(), health.kafka(), health.mysql());
        }
    }

    public record RecentBlockResponse(
            Long familyId, Long customerId, String reason, Instant blockedAt) {

        public static RecentBlockResponse from(AdminDashboard.RecentBlock block) {
            return new RecentBlockResponse(
                    block.familyId(), block.customerId(), block.reason(), block.blockedAt());
        }
    }
}
