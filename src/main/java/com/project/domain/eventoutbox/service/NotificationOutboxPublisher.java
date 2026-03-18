package com.project.domain.eventoutbox.service;

import com.dabom.messaging.kafka.contract.KafkaEventTypes;
import com.dabom.messaging.kafka.contract.KafkaTopics;
import com.dabom.messaging.kafka.event.KafkaEventMessageSupport;
import com.dabom.messaging.kafka.event.dto.EventEnvelope;
import com.dabom.messaging.kafka.event.dto.notification.NotificationEventSupport;
import com.dabom.messaging.kafka.event.dto.notification.NotificationPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.OutboxErrorCode;
import com.project.domain.eventoutbox.entity.EventOutbox;
import com.project.domain.eventoutbox.enums.EventOutboxStatus;
import com.project.domain.eventoutbox.repository.EventOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationOutboxPublisher {

    private final EventOutboxRepository eventOutboxRepository;
    private final EventOutboxStatusService eventOutboxStatusService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaEventMessageSupport kafkaEventMessageSupport;
    private final ObjectMapper objectMapper;

    /** 알림 payload를 outbox에 적재하고, 트랜잭션 커밋 이후 직접 발행을 시도한다. */
    public void enqueueAndPublishAfterCommit(NotificationPayload payload) {
        // eventId를 만들기 위해서만 envelope를 생성하고, DB에는 payload만 저장한다.
        EventEnvelope<NotificationPayload> envelope = NotificationEventSupport.toEnvelope(payload);
        String payloadJson = serializePayload(payload);

        // 동일 eventId가 이미 있으면 중복 적재하지 않는다.
        int inserted =
                eventOutboxRepository.insertPublishPendingIgnore(
                        envelope.eventId(),
                        payload.familyId(),
                        payload.customerId(),
                        payloadJson);

        if (inserted == 0) {
            return;
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // 현재 트랜잭션이 살아 있으면 DB 커밋이 끝난 뒤 발행을 시도한다.
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            tryPublishPending(envelope.eventId());
                        }
                    });
            return;
        }

        // 트랜잭션 바깥에서 호출된 경우에는 즉시 발행을 시도한다.
        tryPublishPending(envelope.eventId());
    }

    /** 아직 PUBLISH_PENDING 상태인 outbox row만 골라 직접 발행을 시도한다. */
    private void tryPublishPending(String eventId) {
        eventOutboxRepository
                .findByEventIdAndDeletedAtIsNull(eventId)
                .filter(outbox -> EventOutboxStatus.PUBLISH_PENDING.equals(outbox.getStatus()))
                .ifPresent(this::publishAndMarkSent);
    }

    /** Kafka 발행이 성공하면 SENT로 바꾸고, 실패하면 PUBLISH_PENDING 상태를 유지한다. */
    private void publishAndMarkSent(EventOutbox outbox) {
        try {
            NotificationPayload payload = deserializePayload(outbox.getPayloadJson());
            EventEnvelope<NotificationPayload> envelope = buildEnvelope(outbox, payload);
            String envelopeJson = kafkaEventMessageSupport.serialize(envelope);

            // 직접 발행 경로는 성공 여부를 즉시 확인해야 하므로 get()으로 ack를 기다린다.
            kafkaTemplate
                    .send(KafkaTopics.NOTIFICATION, outbox.getEventId(), envelopeJson)
                    .get();

            // 성공한 경우에만 SENT로 전이한다.
            eventOutboxStatusService.markSent(outbox.getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn(
                    "Notification publish interrupted. eventId={}, outboxId={}",
                    outbox.getEventId(),
                    outbox.getId(),
                    e);
        } catch (Exception ignored) {
            log.warn(
                    "Notification publish failed. eventId={}, outboxId={}. It will remain PUBLISH_PENDING for retry.",
                    outbox.getEventId(),
                    outbox.getId(),
                    ignored);
        }
    }

    /** outbox payload_json에는 NotificationPayload만 저장한다. */
    private String serializePayload(NotificationPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ApplicationException(OutboxErrorCode.PAYLOAD_SERIALIZATION_FAILED);
        }
    }

    /** outbox에 저장된 NotificationPayload JSON을 다시 객체로 복원한다. */
    private NotificationPayload deserializePayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, NotificationPayload.class);
        } catch (JsonProcessingException e) {
            throw new ApplicationException(OutboxErrorCode.PAYLOAD_SERIALIZATION_FAILED);
        }
    }

    /** Kafka로 보낼 때만 eventId와 eventType을 포함한 envelope를 다시 만든다. */
    private EventEnvelope<NotificationPayload> buildEnvelope(
            EventOutbox outbox, NotificationPayload payload) {
        return new EventEnvelope<>(
                outbox.getEventId(),
                KafkaEventTypes.NOTIFICATION,
                outbox.getCreatedAt(),
                payload);
    }
}
