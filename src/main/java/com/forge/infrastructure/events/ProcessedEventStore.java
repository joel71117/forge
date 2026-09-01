package com.forge.infrastructure.events;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Supplier;

@Repository
public class ProcessedEventStore {
    private final JdbcTemplate jdbcTemplate;

    public ProcessedEventStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public boolean process(UUID eventId, String consumerName, Supplier<String> operation) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO processed_events (event_id, consumer_name, processed_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (event_id, consumer_name) DO NOTHING
                """, eventId, consumerName);
        if (inserted == 0) {
            return false;
        }

        String result = operation.get();
        jdbcTemplate.update("""
                UPDATE processed_events
                SET result = ?
                WHERE event_id = ? AND consumer_name = ?
                """, result, eventId, consumerName);
        return true;
    }
}
