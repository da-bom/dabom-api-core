package com.project.domain.usagerecord;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.project.domain.family.infra.cache.FamilyCacheRepository;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.usagerecord.dto.response.RealtimeUsageResponse;
import com.project.domain.usagerecord.repository.UsageSseEmitterRegistry;
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

    public void pushLatest(UsageRealtimePayload payload) {
        RealtimeUsageResponse response =
                new RealtimeUsageResponse(
                        payload.familyId(),
                        payload.totalUsedBytes(),
                        payload.totalLimitBytes(),
                        payload.remainingBytes());

        registry.send(payload.familyId(), "usage-updated", response);
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

            long latest = latestOpt.get();
            Long prev = lastSeen.putIfAbsent(familyId, latest);

            if (prev == null || prev.longValue() != latest) {
                lastSeen.put(familyId, latest);

                // 임시로 고정
                long totalLimitBytes = 20000;
                UsageRealtimePayload payload =
                        new UsageRealtimePayload(
                                familyId,
                                latest,
                                totalLimitBytes,
                                totalLimitBytes - latest,
                                (double) 30);

                pushLatest(payload);
            }
        }
    }
}
