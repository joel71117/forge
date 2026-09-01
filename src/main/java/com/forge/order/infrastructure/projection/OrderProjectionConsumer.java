package com.forge.order.infrastructure.projection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.common.application.EventEnvelope;
import com.forge.infrastructure.events.ProcessedEventStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "forge.kafka.consumer.enabled", havingValue = "true")
public class OrderProjectionConsumer {
    private static final String CONSUMER_NAME = "order-summary-projection";
    private final ObjectMapper objectMapper;
    private final ProcessedEventStore processedEvents;
    private final OrderSummaryProjection projection;

    public OrderProjectionConsumer(ObjectMapper objectMapper, ProcessedEventStore processedEvents,
            OrderSummaryProjection projection) {
        this.objectMapper = objectMapper;
        this.processedEvents = processedEvents;
        this.projection = projection;
    }

    @KafkaListener(topics = "${forge.kafka.events-topic}", groupId = "order-summary-projection")
    public void consume(String message) throws Exception {
        EventEnvelope event = objectMapper.readValue(message, EventEnvelope.class);
        if (!event.aggregateType().equals("Order")) {
            return;
        }
        JsonNode payload = objectMapper.valueToTree(event.payload());
        processedEvents.process(event.eventId(), CONSUMER_NAME, () -> {
            projection.apply(event, payload);
            return "applied";
        });
    }
}