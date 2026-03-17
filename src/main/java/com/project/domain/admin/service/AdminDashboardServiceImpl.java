package com.project.domain.admin.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.admin.model.AdminDashboard;
import com.project.domain.customer.entity.CustomerQuota;
import com.project.domain.customer.repository.CustomerQuotaRepository;
import com.project.domain.customer.repository.CustomerRepository;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.family.repository.FamilyRepository;
import com.project.domain.usagerecord.repository.UsageRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final CustomerRepository customerRepository;
    private final CustomerQuotaRepository customerQuotaRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final HealthEndpoint healthEndpoint;
    private final Clock clock;

    @Override
    public AdminDashboard getDashboard() {
        LocalDate today = LocalDate.now(clock);
        LocalDate currentMonth = today.withDayOfMonth(1);
        LocalDateTime now = LocalDateTime.now(clock);

        long totalFamilies = familyRepository.countByDeletedAtIsNull();
        long activeFamilies = familyMemberRepository.countDistinctActiveFamilies();
        long totalUsers = customerRepository.countByDeletedAtIsNull();
        long blockedUsers =
                customerQuotaRepository.countByIsBlockedTrueAndCurrentMonthAndDeletedAtIsNull(
                        currentMonth);

        LocalDateTime todayStart = LocalDateTime.of(today, LocalTime.MIN);
        long todayEvents =
                usageRecordRepository.countByEventTimeGreaterThanEqualAndDeletedAtIsNull(
                        todayStart);

        LocalDateTime oneMinuteAgo = now.minusSeconds(60);
        long eventsLastMinute =
                usageRecordRepository.countByEventTimeGreaterThanEqualAndDeletedAtIsNull(
                        oneMinuteAgo);
        long currentTps = eventsLastMinute / 60;

        AdminDashboard.SystemHealth systemHealth = buildSystemHealth();

        List<AdminDashboard.RecentBlock> recentBlocks = buildRecentBlocks(currentMonth);

        return new AdminDashboard(
                totalFamilies,
                activeFamilies,
                totalUsers,
                blockedUsers,
                todayEvents,
                currentTps,
                systemHealth,
                recentBlocks);
    }

    private AdminDashboard.SystemHealth buildSystemHealth() {
        org.springframework.boot.actuate.health.SystemHealth health =
                (org.springframework.boot.actuate.health.SystemHealth) healthEndpoint.health();
        return new AdminDashboard.SystemHealth(
                getComponentStatus(health, "redis"),
                getComponentStatus(health, "kafka"),
                getComponentStatus(health, "db"));
    }

    private String getComponentStatus(
            org.springframework.boot.actuate.health.SystemHealth health, String componentName) {
        HealthComponent component = health.getComponents().get(componentName);
        if (component == null) {
            return "UNKNOWN";
        }
        Status status = component.getStatus();
        return status.getCode();
    }

    private List<AdminDashboard.RecentBlock> buildRecentBlocks(LocalDate currentMonth) {
        List<CustomerQuota> blocked =
                customerQuotaRepository
                        .findTop10ByIsBlockedTrueAndCurrentMonthAndDeletedAtIsNullOrderByUpdatedAtDesc(
                                currentMonth);

        return blocked.stream()
                .map(
                        q ->
                                new AdminDashboard.RecentBlock(
                                        q.getFamilyId(),
                                        q.getCustomerId(),
                                        q.getBlockReason(),
                                        q.getUpdatedAt() != null
                                                ? q.getUpdatedAt()
                                                        .atZone(ZoneId.systemDefault())
                                                        .toInstant()
                                                : null))
                .toList();
    }
}
