package com.forge.infrastructure.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.forge.common.application.EventEnvelope;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.kafka.core.KafkaTemplate;

class EventPublisherTest {
    @Test
    void inMemoryPublisherKeepsAnImmutableEventSnapshot() {
        InMemoryEventPublisher publisher = new InMemoryEventPublisher();
        EventEnvelope event = new EventEnvelope("OrderConfirmed", "order-1", "Order", "payload");

        publisher.publish(event);

        List<EventEnvelope> events = publisher.events();
        assertEquals(List.of(event), events);
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> events.add(event));
    }

    @Test
    void kafkaPublisherUsesConfiguredTopicAndAggregateKey() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, EventEnvelope> kafka = mock(KafkaTemplate.class);
        MockEnvironment environment = new MockEnvironment().withProperty("forge.kafka.events-topic", "orders.v2");
        EventEnvelope event = new EventEnvelope("OrderConfirmed", "order-1", "Order", "payload");

        new KafkaEventPublisher(kafka, environment).publish(event);

        verify(kafka).send("orders.v2", "order-1", event);
    }

    @Test
    void outboxPublisherDelegatesEventsToStore() {
        OutboxEventStore store = mock(OutboxEventStore.class);
        EventEnvelope event = new EventEnvelope("OrderConfirmed", "order-1", "Order", "payload");

        new OutboxEventPublisher(store).publish(event);

        verify(store).append(event);
    }
}