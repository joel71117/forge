package com.forge.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.common.application.EventEnvelope;
import com.forge.infrastructure.events.ProcessedEventStore;
import com.forge.notification.application.NotificationDeliveryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "forge.kafka.consumer.enabled", havingValue = "true")
public class NotificationCreatedConsumer {
    private static final String CONSUMER_NAME = "notification-delivery";

    private final ObjectMapper objectMapper;
    private final ProcessedEventStore processedEvents;
    private final NotificationDeliveryService delivery;

    public NotificationCreatedConsumer(ObjectMapper objectMapper, ProcessedEventStore processedEvents,
            NotificationDeliveryService delivery) {
        this.objectMapper = objectMapper;
        this.processedEvents = processedEvents;
        this.delivery = delivery;
    }

        @RetryableTopic(attempts = "${forge.kafka.retry.attempts:4}",
            backOff = @BackOff(delayString = "${forge.kafka.retry.delay-ms:1000}",
                multiplierString = "${forge.kafka.retry.multiplier:2.0}",
                maxDelayString = "${forge.kafka.retry.max-delay-ms:8000}",
                jitterString = "${forge.kafka.retry.jitter-ms:250}"),
            autoCreateTopics = "true")
    @KafkaListener(topics = "${forge.kafka.events-topic}", groupId = "notification-delivery")
    public void consume(String message) {
        try {
            EventEnvelope event = objectMapper.readValue(message, EventEnvelope.class);
            if (!"NotificationCreated".equals(event.eventType())) return;
            processedEvents.process(event.eventId(), CONSUMER_NAME,
                    () -> delivery.deliver(java.util.UUID.fromString(event.aggregateId())));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not deliver notification", exception);
        }
    }

    @DltHandler
    public void deadLetter(String message, Exception exception) {
        throw new IllegalStateException("Notification delivery event moved to DLT", exception);
    }
}
