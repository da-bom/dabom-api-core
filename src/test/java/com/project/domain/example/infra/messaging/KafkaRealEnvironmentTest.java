package com.project.domain.example.infra.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.project.domain.example.entity.Example;
import com.project.domain.example.service.port.ExampleEventPublisher;

@SpringBootTest
class KafkaRealEnvironmentTest {

    @Autowired private ExampleEventPublisher publisher;

    @MockitoSpyBean private ExampleKafkaConsumer consumer;

    @Test
    @DisplayName("실제 Docker Kafka에 이벤트를 보내고 받는지 확인한다")
    void testRealKafkaFlow() {
        // given
        Example example =
                Example.builder()
                        .exampleId(777L)
                        .exampleName("Real Docker Test 3.4")
                        .exampleContent("Connecting to localhost:9092")
                        .build();

        // when
        System.out.println("[TEST] Sending event to Real Kafka...");
        publisher.publishExampleCreated(example);

        // then
        verify(consumer, timeout(10000).times(1)).consume(any());

        System.out.println("[TEST] Successfully consumed event from Real Kafka!");
    }
}
