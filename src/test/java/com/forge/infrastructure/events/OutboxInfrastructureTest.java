package com.forge.infrastructure.events;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.forge.common.application.EventEnvelope;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

class OutboxInfrastructureTest {
    @Test
    void appendsSerializedEventAndWrapsPersistenceFailure() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ObjectMapper mapper = mapper();
        OutboxEventStore store = new OutboxEventStore(jdbc, mapper);
        EventEnvelope event = new EventEnvelope("OrderConfirmed", "order-1", "Order", "payload");

        store.append(event);

        verify(jdbc).update(anyString(), eq(event.eventId()), eq(event.aggregateType()), eq(event.aggregateId()),
                eq(event.eventType()), eq(event.schemaVersion()), anyString(), any(), any());
        when(jdbc.update(anyString(), any(), any(), any(), any(), any(), anyString(), any(), any()))
                .thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, () -> store.append(event));
    }

    @Test
    void marksPublishedAndFailedWithRetryMetadata() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        OutboxEventStore store = new OutboxEventStore(jdbc, mapper());
        UUID eventId = UUID.randomUUID();
        Instant retryAt = Instant.now().plusSeconds(5);

        store.markPublished(eventId);
        store.markFailed(eventId, new RuntimeException("broker down"), retryAt);

        verify(jdbc).update(anyString(), eq(eventId));
        verify(jdbc).update(anyString(), any(), eq("java.lang.RuntimeException: broker down"), eq(eventId));
    }

    @Test
    void dispatcherPublishesClaimedEvent() throws Exception {
        OutboxEventStore store = mock(OutboxEventStore.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, EventEnvelope> kafka = mock(KafkaTemplate.class);
        ObjectMapper mapper = mapper();
        MockEnvironment environment = new MockEnvironment()
                .withProperty("forge.kafka.events-topic", "forge.events.v2")
                .withProperty("forge.outbox.batch-size", "3");
        EventEnvelope event = new EventEnvelope("OrderConfirmed", "order-1", "Order", "payload");
        ObjectNode payload = mapper.valueToTree(event);
        UUID eventId = event.eventId();
        when(store.claim(3)).thenReturn(List.of(new OutboxEventStore.PendingEvent(eventId, payload, 0)));
        when(kafka.send("forge.events.v2", "order-1", event)).thenReturn(CompletableFuture.completedFuture(null));

        new OutboxDispatcher(store, kafka, mapper, environment).dispatch();

        verify(store).markPublished(eventId);
    }

    @Test
    void dispatcherMarksKafkaFailureForRetry() {
        OutboxEventStore store = mock(OutboxEventStore.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, EventEnvelope> kafka = mock(KafkaTemplate.class);
        ObjectMapper mapper = mapper();
        MockEnvironment environment = new MockEnvironment();
        EventEnvelope event = new EventEnvelope("OrderConfirmed", "order-1", "Order", "payload");
        when(store.claim(50)).thenReturn(List.of(new OutboxEventStore.PendingEvent(
                event.eventId(), mapper.valueToTree(event), 4)));
        when(kafka.send(anyString(), anyString(), any(EventEnvelope.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        new OutboxDispatcher(store, kafka, mapper, environment).dispatch();

        verify(store).markFailed(eq(event.eventId()), any(Exception.class), any(Instant.class));
    }

    private static ObjectMapper mapper() {
        SimpleModule timeModule = new SimpleModule();
        timeModule.addSerializer(Instant.class, new JsonSerializer<>() {
            @Override
            public void serialize(Instant value, JsonGenerator generator,
                    com.fasterxml.jackson.databind.SerializerProvider provider) throws java.io.IOException {
                generator.writeString(value.toString());
            }
        });
        timeModule.addDeserializer(Instant.class, new JsonDeserializer<>() {
            @Override
            public Instant deserialize(JsonParser parser,
                    com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                return Instant.parse(parser.getValueAsString());
            }
        });
        return new ObjectMapper().registerModule(timeModule);
    }
}