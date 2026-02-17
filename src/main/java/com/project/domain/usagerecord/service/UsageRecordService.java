package com.project.domain.usagerecord.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.project.domain.customer.repository.CustomerQuotaQueryRepository;
import com.project.domain.family.infra.cache.FamilyCacheRepository;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.usagerecord.dto.response.CustomerUsageResponse;
import com.project.domain.usagerecord.dto.response.RealtimeFamilyUsageResponse;
import com.project.domain.usagerecord.dto.response.RealtimeTotalUsageResponse;
import com.project.domain.usagerecord.infra.sse.UsageSseEmitterRegistry;
import com.project.global.event.dto.usage.UsageRealtimePayload;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.FamilyErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageRecordService {

    private final UsageSseEmitterRegistry registry;
    private final FamilyCacheRepository familyCacheRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final CustomerQuotaQueryRepository customerQuotaQueryRepository;

    private final ConcurrentHashMap<Long, Long> lastSeen = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long customerId) {
        Long familyId =
                familyMemberRepository
                        .findFamilyIdByCustomerId(customerId)
                        .orElseThrow(
                                () -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND));

        return registry.register(familyId);
    }

    public void pushTotalUsageBytes(UsageRealtimePayload payload) {
        RealtimeTotalUsageResponse response =
                new RealtimeTotalUsageResponse(
                        payload.familyId(),
                        payload.totalUsedBytes(),
                        payload.totalLimitBytes(),
                        payload.remainingBytes());

        registry.send(payload.familyId(), "usage-updated", response);
    }

    public void pushMemberUsageBytes(UsageRealtimePayload payload) {
        Long familyId = payload.familyId();
        Long customerId = payload.customerId();

        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        // 전체 가족 구성원의 monthlyLimitBytes를 데이터베이스에서 조회한다
        List<CustomerUsageResponse> customerUsageResponses =
                customerQuotaQueryRepository.findCustomerUsage(familyId, customerId);

        // 이벤트 대상이 아닌 가족 구성원들의 monthlyUsedBytes를 캐시에서 조회한다
        for (CustomerUsageResponse customerUsageResponse : customerUsageResponses) {
            long componentId = customerUsageResponse.getCustomerId();

            if (componentId != customerId) {
                Optional<Long> usageOpt =
                        familyCacheRepository.findCustomerMonthlyUsageBytes(familyId, componentId);

                Long usageByte = usageOpt.orElse(0L);
                customerUsageResponse.setMonthlyUsedBytes(usageByte);

            } else if (componentId == customerId) {
                customerUsageResponse.setMonthlyUsedBytes(payload.monthlyUsedBytes());
            }
        }

        // dto를 조합한다
        RealtimeFamilyUsageResponse response =
                new RealtimeFamilyUsageResponse(familyId, year, month, customerUsageResponses);
        registry.send(familyId, "usage-updated-by-member", response);
    }

    /** 스케줄링을 통한 push 테스트 메소드 */
    @Scheduled(fixedDelay = 1000)
    public void pollAndPushIfChanged() {
        for (Long familyId : registry.activeFamilyIds()) {

            Optional<Long> latestOpt = familyCacheRepository.findFamilyRemainingBytes(familyId);
            if (latestOpt.isEmpty()) {
                lastSeen.remove(familyId);
                continue;
            }

            long remainingBytes = latestOpt.get();
            Long prev = lastSeen.putIfAbsent(familyId, remainingBytes);

            if (prev == null || prev.longValue() != remainingBytes) {
                lastSeen.put(familyId, remainingBytes);

                // 임시로 고정
                long totalLimitBytes = 20000;
                long totalUsedBytes = totalLimitBytes - remainingBytes;
                UsageRealtimePayload payload =
                        new UsageRealtimePayload(
                                familyId,
                                1L,
                                totalUsedBytes,
                                totalLimitBytes,
                                remainingBytes,
                                30.0,
                                null,
                                null,
                                null);

                pushTotalUsageBytes(payload);
                pushMemberUsageBytes(payload);
            }
        }
    }
}
