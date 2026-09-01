package com.forge.common.application;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope(
        UUID eventId,
        String eventType,
    int schemaVersion,
        String aggregateId,
        String aggregateType,
        Instant occurredAt,
        String correlationId,
        String causationId,
        Object payload) {
    public EventEnvelope(String eventType, String aggregateId, String aggregateType, Object payload) {
        this(UUID.randomUUID(), eventType, 1, aggregateId, aggregateType, Instant.now(), null, null, payload);
    }
}