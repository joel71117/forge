package com.forge.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.common.application.EventEnvelope;
import com.forge.infrastructure.events.ProcessedEventStore;
import com.forge.notification.api.dto.CreateNotificationRequest;
import com.forge.notification.application.NotificationService;
import com.forge.notification.domain.NotificationChannel;
import com.forge.notification.domain.NotificationPriority;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "forge.notification.local-consumer.enabled", havingValue = "true", matchIfMissing = true)
public class OrderConfirmedNotificationConsumer {
    private static final String CONSUMER_NAME = "order-confirmed-notification";

    private final ObjectMapper objectMapper;
    private final ProcessedEventStore processedEvents;
    private final NotificationService notifications;

    public OrderConfirmedNotificationConsumer(ObjectMapper objectMapper, ProcessedEventStore processedEvents,
            NotificationService notifications) {
        this.objectMapper = objectMapper;
        this.processedEvents = processedEvents;
        this.notifications = notifications;
    }

    @RetryableTopic(attempts = "${forge.kafka.retry.attempts:4}", backOff = @BackOff(delayString = "${forge.kafka.retry.delay-ms:1000}", multiplierString = "${forge.kafka.retry.multiplier:2.0}", maxDelayString = "${forge.kafka.retry.max-delay-ms:8000}", jitterString = "${forge.kafka.retry.jitter-ms:250}"), autoCreateTopics = "true")
    @KafkaListener(topics = "${forge.kafka.events-topic}", groupId = "notification-workers")
    public void consume(String message) {
        try {
            EventEnvelope event = objectMapper.readValue(message, EventEnvelope.class);
            if (!"OrderConfirmed".equals(event.eventType()))
                return;
            var payload = objectMapper.convertValue(event.payload(), com.fasterxml.jackson.databind.JsonNode.class);
            var request = new CreateNotificationRequest();
            request.customerId = payload.get("customerId").asText();
            request.type = "ORDER_CONFIRMED";
            request.channel = NotificationChannel.EMAIL;
            request.priority = NotificationPriority.NORMAL;
            processedEvents.process(event.eventId(), CONSUMER_NAME,
                    () -> notifications.create("order-confirmed:" + event.aggregateId(), request).getId().toString());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create order notification", exception);
        }
    }

    @DltHandler
    public void deadLetter(String message, Exception exception) {
        throw new IllegalStateException("Order notification event moved to DLT", exception);
    }
}
