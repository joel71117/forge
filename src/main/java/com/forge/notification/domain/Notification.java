package com.forge.notification.domain;

import com.forge.commerce.common.IdempotencyKey;

import java.util.Objects;
import java.util.UUID;

/**
 * Notification is a stateful domain concept with retry semantics.
 *
 * <p>Retrying a notification should not be a free-form field mutation; it should be a valid
 * change in status and intent. The state machine makes those rules visible.</p>
 */
public class Notification {
    private final NotificationId id;
    private final UUID customerId;
    private final String type;
    private final NotificationChannel channel;
    private final NotificationPriority priority;
    private final IdempotencyKey idempotencyKey;
    private NotificationStatus status;

    public Notification(UUID customerId, String type, NotificationChannel channel,
                        NotificationPriority priority, IdempotencyKey idempotencyKey) {
        if (customerId == null) {
            throw new IllegalArgumentException("CustomerId cannot be null.");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Notification type cannot be blank.");
        }
        if (channel == null) {
            throw new IllegalArgumentException("Channel cannot be null.");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Priority cannot be null.");
        }
        if (idempotencyKey == null) {
            throw new IllegalArgumentException("Idempotency key cannot be null.");
        }

        this.id = new NotificationId(UUID.randomUUID());
        this.customerId = customerId;
        this.type = type.trim();
        this.channel = channel;
        this.priority = priority;
        this.idempotencyKey = idempotencyKey;
        this.status = NotificationStatus.PENDING;
    }

    public NotificationId getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getType() {
        return type;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public NotificationPriority getPriority() {
        return priority;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public IdempotencyKey getIdempotencyKey() {
        return idempotencyKey;
    }

    public void startProcessing() {
        if (status != NotificationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING notifications can start processing.");
        }
        this.status = NotificationStatus.PROCESSING;
    }

    public void markSent() {
        if (status != NotificationStatus.PROCESSING) {
            throw new IllegalStateException("Only PROCESSING notifications can be sent.");
        }
        this.status = NotificationStatus.SENT;
    }

    public void markFailed() {
        if (status != NotificationStatus.PROCESSING) {
            throw new IllegalStateException("Only PROCESSING notifications can fail.");
        }
        this.status = NotificationStatus.FAILED;
    }

    public void scheduleRetry() {
        if (status != NotificationStatus.FAILED) {
            throw new IllegalStateException("Only FAILED notifications can be retried.");
        }
        this.status = NotificationStatus.RETRYING;
    }

    public void deadLetter() {
        if (status != NotificationStatus.FAILED) {
            throw new IllegalStateException("Only FAILED notifications can be dead-lettered.");
        }
        this.status = NotificationStatus.DEAD_LETTERED;
    }

    public void cancel() {
        if (status == NotificationStatus.SENT || status == NotificationStatus.CANCELLED) {
            throw new IllegalStateException("This notification cannot be cancelled in its current state.");
        }
        this.status = NotificationStatus.CANCELLED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
