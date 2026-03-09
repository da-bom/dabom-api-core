package com.project.domain.recap.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.family.entity.Family;
import com.project.domain.family.service.FamilyService;
import com.project.domain.recap.entity.FamilyRecapMonthly;
import com.project.domain.recap.model.MonthlyRecap;
import com.project.domain.recap.repository.FamilyRecapMonthlyRepository;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.RecapErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecapServiceImpl implements RecapService {

    private final FamilyRecapMonthlyRepository familyRecapMonthlyRepository;
    private final FamilyService familyService;
    private final ObjectMapper objectMapper;

    @Override
    public MonthlyRecap getMonthlyRecap(Long customerId, int year, int month) {
        Long familyId = familyService.getFamilyIdByCustomerId(customerId);
        LocalDate reportMonth = LocalDate.of(year, month, 1);

        FamilyRecapMonthly recap =
                familyRecapMonthlyRepository
                        .findByFamilyIdAndReportMonthAndDeletedAtIsNull(familyId, reportMonth)
                        .orElseThrow(
                                () -> new ApplicationException(RecapErrorCode.RECAP_NOT_FOUND));

        Family family = familyService.getFamilyById(familyId);

        return new MonthlyRecap(
                recap.getId(),
                recap.getFamilyId(),
                family.getName(),
                recap.getReportMonth(),
                recap.getTotalUsedBytes(),
                recap.getTotalQuotaBytes(),
                recap.getUsageRatePercent(),
                readSnapshot(
                        recap.getUsageByWeekday(),
                        MonthlyRecap.UsageByWeekday.class,
                        MonthlyRecap.UsageByWeekday.empty()),
                readSnapshot(
                        recap.getPeakUsage(),
                        MonthlyRecap.PeakUsage.class,
                        MonthlyRecap.PeakUsage.empty()),
                readSnapshot(
                        recap.getMissionSummaryJson(),
                        MonthlyRecap.MissionSummary.class,
                        MonthlyRecap.MissionSummary.empty()),
                readSnapshot(
                        recap.getAppealSummaryJson(),
                        MonthlyRecap.AppealSummary.class,
                        MonthlyRecap.AppealSummary.empty()),
                readSnapshot(
                        recap.getAppealHighlightsJson(),
                        MonthlyRecap.AppealHighlights.class,
                        MonthlyRecap.AppealHighlights.empty()),
                recap.getCommunicationScore(),
                resolveGeneratedAt(recap));
    }

    private LocalDateTime resolveGeneratedAt(FamilyRecapMonthly recap) {
        return recap.getUpdatedAt() != null ? recap.getUpdatedAt() : recap.getCreatedAt();
    }

    private <T> T readSnapshot(String json, Class<T> type, T defaultValue) {
        if (json == null || json.isBlank()) {
            return defaultValue;
        }
        try {
            T value = objectMapper.readValue(json, type);
            return value == null ? defaultValue : value;
        } catch (JsonProcessingException e) {
            throw new ApplicationException(RecapErrorCode.RECAP_JSON_DESERIALIZATION_FAILED);
        }
    }
}
