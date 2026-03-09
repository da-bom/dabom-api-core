package com.project.domain.recap.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    public record UsageByWeekday(
            Double monday,
            Double tuesday,
            Double wednesday,
            Double thursday,
            Double friday,
            Double saturday,
            Double sunday) {

        public UsageByWeekday {
            monday = defaultToZero(monday);
            tuesday = defaultToZero(tuesday);
            wednesday = defaultToZero(wednesday);
            thursday = defaultToZero(thursday);
            friday = defaultToZero(friday);
            saturday = defaultToZero(saturday);
            sunday = defaultToZero(sunday);
        }

        public static UsageByWeekday empty() {
            return new UsageByWeekday(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        private static Double defaultToZero(Double value) {
            return value == null ? 0.0 : value;
        }
    }

    public record PeakUsage(Integer startHour, Integer endHour, String mostUsedWeekday) {
        public static PeakUsage empty() {
            return new PeakUsage(null, null, null);
        }
    }

    public record MissionSummary(
            Integer totalMissionCount,
            Integer completedMissionCount,
            Integer rejectedRequestCount) {

        public MissionSummary {
            totalMissionCount = defaultToZero(totalMissionCount);
            completedMissionCount = defaultToZero(completedMissionCount);
            rejectedRequestCount = defaultToZero(rejectedRequestCount);
        }

        public static MissionSummary empty() {
            return new MissionSummary(0, 0, 0);
        }
    }

    public record AppealSummary(
            Integer totalAppeals, Integer approvedAppeals, Integer rejectedAppeals) {

        public AppealSummary {
            totalAppeals = defaultToZero(totalAppeals);
            approvedAppeals = defaultToZero(approvedAppeals);
            rejectedAppeals = defaultToZero(rejectedAppeals);
        }

        public static AppealSummary empty() {
            return new AppealSummary(0, 0, 0);
        }
    }

    public record AppealHighlights(
            TopSuccessfulRequester topSuccessfulRequester,
            TopAcceptedApprover topAcceptedApprover) {

        public AppealHighlights {
            topSuccessfulRequester =
                    topSuccessfulRequester == null
                            ? TopSuccessfulRequester.empty()
                            : topSuccessfulRequester;
            topAcceptedApprover =
                    topAcceptedApprover == null ? TopAcceptedApprover.empty() : topAcceptedApprover;
        }

        public static AppealHighlights empty() {
            return new AppealHighlights(null, null);
        }
    }

    public record TopSuccessfulRequester(
            Long requesterId,
            String requesterName,
            Integer approvedAppealCount,
            List<RecentApprovedAppeal> recentApprovedAppeals) {

        public TopSuccessfulRequester {
            approvedAppealCount = defaultToZero(approvedAppealCount);
            recentApprovedAppeals =
                    recentApprovedAppeals == null ? List.of() : List.copyOf(recentApprovedAppeals);
        }

        public static TopSuccessfulRequester empty() {
            return new TopSuccessfulRequester(null, null, 0, List.of());
        }
    }

    public record RecentApprovedAppeal(
            Long appealId,
            Long approverId,
            String approverName,
            String requestReason,
            LocalDateTime requestedAt) {}

    public record TopAcceptedApprover(
            Long approverId,
            String approverName,
            Integer approvedAppealCount,
            List<RecentAcceptedAppeal> recentAcceptedAppeals) {

        public TopAcceptedApprover {
            approvedAppealCount = defaultToZero(approvedAppealCount);
            recentAcceptedAppeals =
                    recentAcceptedAppeals == null ? List.of() : List.copyOf(recentAcceptedAppeals);
        }

        public static TopAcceptedApprover empty() {
            return new TopAcceptedApprover(null, null, 0, List.of());
        }
    }

    public record RecentAcceptedAppeal(
            Long appealId,
            Long requesterId,
            String requesterName,
            String requestReason,
            LocalDateTime resolvedAt) {}

    private static Integer defaultToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
