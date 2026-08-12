package com.forge.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class Notification {
    private final NotificationId id;
    private final UUID customerId;
    private String type;
    private NotificationChannel channel;
    private int priority;
    private String template;
    private Map<String, Object> payload;
    private NotificationStatus status;
    private Instant scheduledAt;
    private String idempotencyKey;
    private final Instant createdAt;
    private Instant updatedAt;

    public Notification(UUID customerId, String type, NotificationChannel channel, int priority,
                        String template, Map<String, Object> payload, NotificationStatus status,
                        Instant scheduledAt, String idempotencyKey, Instant createdAt, Instant updatedAt) {
        this.id = new NotificationId(UUID.randomUUID());
        this.customerId = customerId;
        this.type = type;
        this.channel = channel;
        this.priority = priority;
        this.template = template;
        this.payload = payload;
        this.status = status;
        this.scheduledAt = scheduledAt;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
