package com.forge.order.infrastructure.projection;

import com.fasterxml.jackson.databind.JsonNode;
import com.forge.common.application.EventEnvelope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public class OrderSummaryProjection {
    private final JdbcTemplate jdbcTemplate;

    public OrderSummaryProjection(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void apply(EventEnvelope event, JsonNode payload) {
        long version = payload.path("aggregateVersion").asLong();
        if ("OrderConfirmed".equals(event.eventType())) {
            jdbcTemplate.update("""
                    INSERT INTO order_summary
                    (order_id, customer_id, status, total_amount, item_count, aggregate_version, last_event_id, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    ON CONFLICT (order_id) DO UPDATE
                    SET customer_id = EXCLUDED.customer_id, status = EXCLUDED.status,
                        total_amount = EXCLUDED.total_amount, item_count = EXCLUDED.item_count,
                        aggregate_version = EXCLUDED.aggregate_version, last_event_id = EXCLUDED.last_event_id,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE order_summary.aggregate_version < EXCLUDED.aggregate_version
                    """, UUID.fromString(event.aggregateId()), UUID.fromString(payload.path("customerId").asText()),
                    payload.path("status").asText(), new BigDecimal(payload.path("totalAmount").asText()),
                    payload.path("itemCount").asInt(), version, event.eventId());
            return;
        }
        if ("OrderCancelled".equals(event.eventType())) {
            jdbcTemplate.update("""
                    UPDATE order_summary
                    SET status = ?, aggregate_version = ?, last_event_id = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE order_id = ? AND aggregate_version < ?
                    """, payload.path("status").asText(), version, event.eventId(),
                    UUID.fromString(event.aggregateId()), version);
        }
    }

    public void clear() {
        jdbcTemplate.update("DELETE FROM order_summary");
    }
}