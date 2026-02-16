package com.project.domain.usagerecord.infra.event.handler;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.project.domain.usagerecord.infra.event.RealtimeUsageMemberEvent;
import com.project.domain.usagerecord.service.UsageRecordService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeUsageNotifyHandler {

    private final UsageRecordService usageRecordService;

    @EventListener
    @Async
    public void handle(RealtimeUsageMemberEvent event) {
        try {
            usageRecordService.pushMemberUsageBytes(event.payload());
            log.info("RealtimeUsageNotifyEvent 처리 완료");
        } catch (Exception e) {
            log.error("RealtimeUsageNotifyEvent 처리 실패: familyId={}", event.payload().familyId(), e);
        }
    }
}
