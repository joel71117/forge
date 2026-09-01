package com.forge.infrastructure.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.common.application.EventEnvelope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class OutboxEventStore {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public OutboxEventStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void append(EventEnvelope event) {
        try {
            var now = Instant.now();
            jdbcTemplate.update("""
                    INSERT INTO outbox_events
                    (id, aggregate_type, aggregate_id, event_type, schema_version, payload,
                     created_at, attempt_count, next_attempt_at, status)
                    VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, 0, ?, 'PENDING')
                    """,
                    event.eventId(), event.aggregateType(), event.aggregateId(), event.eventType(),
                    event.schemaVersion(), objectMapper.writeValueAsString(event), Timestamp.from(event.occurredAt()),
                    Timestamp.from(now));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not append event to outbox", exception);
        }
    }

    @Transactional
    public List<PendingEvent> claim(int batchSize) {
        var events = jdbcTemplate.query("""
                SELECT id, payload, attempt_count
                FROM outbox_events
                     WHERE ((status IN ('PENDING', 'FAILED') AND next_attempt_at <= CURRENT_TIMESTAMP)
                         OR (status = 'PROCESSING' AND claimed_at < CURRENT_TIMESTAMP - INTERVAL '1 minute'))
                ORDER BY created_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
                """, (resultSet, rowNumber) -> new PendingEvent(
                resultSet.getObject("id", UUID.class),
                readPayload(resultSet.getString("payload")),
                resultSet.getInt("attempt_count")), batchSize);
        events.forEach(event -> jdbcTemplate.update(
                "UPDATE outbox_events SET status = 'PROCESSING', claimed_at = CURRENT_TIMESTAMP WHERE id = ?", event.id()));
        return events;
    }

    public void markPublished(UUID id) {
        jdbcTemplate.update("""
                UPDATE outbox_events
                SET status = 'PUBLISHED', published_at = CURRENT_TIMESTAMP, claimed_at = NULL, last_error = NULL
                WHERE id = ?
                """, id);
    }

    public void markFailed(UUID id, Exception exception, Instant nextAttemptAt) {
        jdbcTemplate.update("""
                UPDATE outbox_events
                SET status = 'FAILED', attempt_count = attempt_count + 1,
                    next_attempt_at = ?, claimed_at = NULL, last_error = ?
                WHERE id = ?
                """, Timestamp.from(nextAttemptAt), exception.toString(), id);
    }

    public List<JsonNode> historyFor(String aggregateType) {
        return jdbcTemplate.query("""
                SELECT payload FROM outbox_events
                WHERE aggregate_type = ? AND status = 'PUBLISHED'
                ORDER BY created_at, id
                """, (resultSet, rowNumber) -> readPayload(resultSet.getString("payload")), aggregateType);
    }

    public int pendingCount() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM outbox_events
                WHERE status IN ('PENDING', 'FAILED', 'PROCESSING')
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private JsonNode readPayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read outbox payload", exception);
        }
    }

    public record PendingEvent(UUID id, JsonNode payload, int attemptCount) {
    }
}
