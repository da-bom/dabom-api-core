package com.project.domain.recap.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.project.domain.recap.model.MonthlyRecap;

public record MonthlyRecapResponse(
        Long recapId,
        Long familyId,
        String familyName,
        LocalDate reportMonth,
        Long totalUsedBytes,
        Long totalQuotaBytes,
        BigDecimal usageRatePercent,
        UsageByWeekday usageByWeekday,
        PeakUsage peakUsage,
        MissionSummary missionSummary,
        AppealSummary appealSummary,
        AppealHighlights appealHighlights,
        BigDecimal communicationScore,
        LocalDateTime generatedAt) {

    public static MonthlyRecapResponse from(MonthlyRecap monthlyRecap) {
        return new MonthlyRecapResponse(
                monthlyRecap.recapId(),
                monthlyRecap.familyId(),
                monthlyRecap.familyName(),
                monthlyRecap.reportMonth(),
                monthlyRecap.totalUsedBytes(),
                monthlyRecap.totalQuotaBytes(),
                monthlyRecap.usageRatePercent(),
                UsageByWeekday.from(monthlyRecap.usageByWeekday()),
                PeakUsage.from(monthlyRecap.peakUsage()),
                MissionSummary.from(monthlyRecap.missionSummary()),
                AppealSummary.from(monthlyRecap.appealSummary()),
                AppealHighlights.from(monthlyRecap.appealHighlights()),
                monthlyRecap.communicationScore(),
                monthlyRecap.generatedAt());
    }

    public record UsageByWeekday(
            Double monday,
            Double tuesday,
            Double wednesday,
            Double thursday,
            Double friday,
            Double saturday,
            Double sunday) {

        public static UsageByWeekday from(MonthlyRecap.UsageByWeekday usageByWeekday) {
            return new UsageByWeekday(
                    usageByWeekday.monday(),
                    usageByWeekday.tuesday(),
                    usageByWeekday.wednesday(),
                    usageByWeekday.thursday(),
                    usageByWeekday.friday(),
                    usageByWeekday.saturday(),
                    usageByWeekday.sunday());
        }
    }

    public record PeakUsage(Integer startHour, Integer endHour, String mostUsedWeekday) {
        public static PeakUsage from(MonthlyRecap.PeakUsage peakUsage) {
            return new PeakUsage(
                    peakUsage.startHour(), peakUsage.endHour(), peakUsage.mostUsedWeekday());
        }
    }

    public record MissionSummary(
            Integer totalMissionCount,
            Integer completedMissionCount,
            Integer rejectedRequestCount) {

        public static MissionSummary from(MonthlyRecap.MissionSummary missionSummary) {
            return new MissionSummary(
                    missionSummary.totalMissionCount(),
                    missionSummary.completedMissionCount(),
                    missionSummary.rejectedRequestCount());
        }
    }

    public record AppealSummary(
            Integer totalAppeals, Integer approvedAppeals, Integer rejectedAppeals) {

        public static AppealSummary from(MonthlyRecap.AppealSummary appealSummary) {
            return new AppealSummary(
                    appealSummary.totalAppeals(),
                    appealSummary.approvedAppeals(),
                    appealSummary.rejectedAppeals());
        }
    }

    public record AppealHighlights(
            TopSuccessfulRequester topSuccessfulRequester,
            TopAcceptedApprover topAcceptedApprover) {

        public static AppealHighlights from(MonthlyRecap.AppealHighlights appealHighlights) {
            return new AppealHighlights(
                    TopSuccessfulRequester.from(appealHighlights.topSuccessfulRequester()),
                    TopAcceptedApprover.from(appealHighlights.topAcceptedApprover()));
        }
    }

    public record TopSuccessfulRequester(
            Long requesterId,
            String requesterName,
            Integer approvedAppealCount,
            List<RecentApprovedAppeal> recentApprovedAppeals) {

        public static TopSuccessfulRequester from(
                MonthlyRecap.TopSuccessfulRequester topSuccessfulRequester) {
            return new TopSuccessfulRequester(
                    topSuccessfulRequester.requesterId(),
                    topSuccessfulRequester.requesterName(),
                    topSuccessfulRequester.approvedAppealCount(),
                    topSuccessfulRequester.recentApprovedAppeals().stream()
                            .map(RecentApprovedAppeal::from)
                            .toList());
        }
    }

    public record RecentApprovedAppeal(
            Long appealId,
            Long approverId,
            String approverName,
            String requestReason,
            LocalDateTime requestedAt) {

        public static RecentApprovedAppeal from(
                MonthlyRecap.RecentApprovedAppeal recentApprovedAppeal) {
            return new RecentApprovedAppeal(
                    recentApprovedAppeal.appealId(),
                    recentApprovedAppeal.approverId(),
                    recentApprovedAppeal.approverName(),
                    recentApprovedAppeal.requestReason(),
                    recentApprovedAppeal.requestedAt());
        }
    }

    public record TopAcceptedApprover(
            Long approverId,
            String approverName,
            Integer approvedAppealCount,
            List<RecentAcceptedAppeal> recentAcceptedAppeals) {

        public static TopAcceptedApprover from(
                MonthlyRecap.TopAcceptedApprover topAcceptedApprover) {
            return new TopAcceptedApprover(
                    topAcceptedApprover.approverId(),
                    topAcceptedApprover.approverName(),
                    topAcceptedApprover.approvedAppealCount(),
                    topAcceptedApprover.recentAcceptedAppeals().stream()
                            .map(RecentAcceptedAppeal::from)
                            .toList());
        }
    }

    public record RecentAcceptedAppeal(
            Long appealId,
            Long requesterId,
            String requesterName,
            String requestReason,
            LocalDateTime resolvedAt) {

        public static RecentAcceptedAppeal from(
                MonthlyRecap.RecentAcceptedAppeal recentAcceptedAppeal) {
            return new RecentAcceptedAppeal(
                    recentAcceptedAppeal.appealId(),
                    recentAcceptedAppeal.requesterId(),
                    recentAcceptedAppeal.requesterName(),
                    recentAcceptedAppeal.requestReason(),
                    recentAcceptedAppeal.resolvedAt());
        }
    }
}
