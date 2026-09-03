package com.forge.notificationservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationWorker {
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbc;

    public NotificationWorker(ObjectMapper objectMapper, JdbcTemplate jdbc) {
        this.objectMapper = objectMapper;
        this.jdbc = jdbc;
    }

    @KafkaListener(topics = "${FORGE_KAFKA_EVENTS_TOPIC:forge.learning.events}")
    public void consume(String message) throws Exception {
        JsonNode event = objectMapper.readTree(message);
        if (!"OrderConfirmed".equals(event.path("eventType").asText()))
            return;
        JsonNode payload = event.path("payload");
        UUID eventId = UUID.fromString(event.path("eventId").asText());
        int inserted = jdbc.update("""
                INSERT INTO notification_attempts(event_id, order_id, customer_id, channel, status)
                VALUES (?, ?, ?, 'EMAIL', 'SENT') ON CONFLICT (event_id) DO NOTHING
                """, eventId, event.path("aggregateId").asText(), payload.path("customerId").asText());
        if (inserted == 1) {
            jdbc.update("UPDATE notification_attempts SET provider_reference = ? WHERE event_id = ?",
                    "local-provider-" + eventId, eventId);
        }
    }
}
