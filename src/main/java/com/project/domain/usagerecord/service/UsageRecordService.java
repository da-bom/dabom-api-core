package com.project.domain.usagerecord.service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.project.domain.family.infra.cache.FamilyCacheRepository;
import com.project.domain.family.repository.FamilyMemberRepository;
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
                                null,
                                totalUsedBytes,
                                totalLimitBytes,
                                remainingBytes,
                                30.0,
                                null,
                                null,
                                null);

                pushTotalUsageBytes(payload);
            }
        }
    }
}
