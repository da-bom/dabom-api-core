package com.project.domain.policy.infra.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.project.global.event.dto.EventEnvelope;
import com.project.global.event.dto.policy.PolicyUpdatedPayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyUpdateKafkaProducer implements PolicyUpdateEventPublish {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "policy-updated";
    private static final String EVENT_TYPE = "POLICY_UPDATED";

    @Override
    public void publish(PolicyUpdatedPayload payload) {

        EventEnvelope<PolicyUpdatedPayload> envelope = EventEnvelope.of(EVENT_TYPE, payload);

        kafkaTemplate.send(TOPIC, payload);

        log.info(
                "Published PolicyUpdate event: {} (PolicyKey: {})",
                envelope.eventId(),
                payload.policyKey());
    }
}
