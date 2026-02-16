package com.project.domain.usagerecord.infra.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.usagerecord.service.UsageRecordService;
import com.project.global.event.dto.EventEnvelope;
import com.project.global.event.dto.usage.UsageRealtimePayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsageRecordKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final UsageRecordService usageRecordService;

    @KafkaListener(topics = "usage-realtime", groupId = "usage-service")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            EventEnvelope<UsageRealtimePayload> envelope =
                    objectMapper.readValue(
                            record.value(),
                            new TypeReference<EventEnvelope<UsageRealtimePayload>>() {});

            UsageRealtimePayload payload = envelope.payload();

            log.info(
                    "FamilyId:{}, totalUsedBytes:{}", payload.familyId(), payload.totalUsedBytes());

            usageRecordService.pushTotalUsageBytes(payload);
        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 실패", e);
        }
    }
}
