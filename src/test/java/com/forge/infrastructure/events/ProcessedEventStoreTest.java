package com.forge.infrastructure.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class ProcessedEventStoreTest {
    @Test
    void processesNewEventAndPersistsOperationResult() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        UUID eventId = UUID.randomUUID();
        when(jdbc.update(anyString(), org.mockito.ArgumentMatchers.eq(eventId),
                org.mockito.ArgumentMatchers.eq("order-projection"))).thenReturn(1);
        ProcessedEventStore store = new ProcessedEventStore(jdbc);

        assertTrue(store.process(eventId, "order-projection", () -> "projection-42"));

        verify(jdbc).update(anyString(), "projection-42", eventId, "order-projection");
    }

    @Test
    void skipsDuplicateEventWithoutRunningOperation() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        UUID eventId = UUID.randomUUID();
        when(jdbc.update(anyString(), eventId, "notification-worker")).thenReturn(0);
        AtomicBoolean ran = new AtomicBoolean();
        ProcessedEventStore store = new ProcessedEventStore(jdbc);

        assertFalse(store.process(eventId, "notification-worker", () -> {
            ran.set(true);
            return "should-not-run";
        }));

        assertFalse(ran.get());
        verifyNoMoreInteractions(jdbc);
    }

    @Test
    void propagatesOperationFailureWithoutWritingAResult() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        UUID eventId = UUID.randomUUID();
        when(jdbc.update(anyString(), eventId, "consumer")).thenReturn(1);
        ProcessedEventStore store = new ProcessedEventStore(jdbc);

        assertEquals("handler failed", org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> store.process(eventId, "consumer", () -> {
                    throw new IllegalStateException("handler failed");
                }))
                .getMessage());

        verify(jdbc).update(anyString(), eventId, "consumer");
    }
}