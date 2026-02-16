package com.project.domain.usagerecord.infra.event.handler;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.project.domain.usagerecord.infra.event.TotalUsageBytesUpdateEvent;
import com.project.domain.usagerecord.service.UsageRecordService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TotalUsageBytesUpdateHandler {

    private final UsageRecordService usageRecordService;

    @EventListener
    @Async
    public void handle(TotalUsageBytesUpdateEvent event) {
        try {
            log.info("TotalUsageBytesUpdateEvent 처리 완료");
            usageRecordService.pushTotalUsageBytes(event.payload());
        } catch (Exception e) {
            log.error(
                    "TotalUsageBytesUpdateEvent 처리 실패: familyId={}", event.payload().familyId(), e);
        }
    }
}
