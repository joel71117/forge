package com.forge.notification.application;

import com.forge.common.api.ConflictException;
import com.forge.common.api.ResourceNotFoundException;
import com.forge.common.application.EventEnvelope;
import com.forge.common.application.EventPublisher;
import com.forge.commerce.common.IdempotencyKey;
import com.forge.notification.application.port.NotificationRepository;
import com.forge.notification.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import com.forge.infrastructure.idempotency.IdempotencyRequestStore;
import com.forge.infrastructure.idempotency.RequestFingerprint;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class NotificationService {
    private final NotificationRepository repository;
    private final EventPublisher eventPublisher;
    private final ObjectProvider<IdempotencyRequestStore> idempotencyStore;

    public NotificationService(NotificationRepository repository, EventPublisher eventPublisher,
            ObjectProvider<IdempotencyRequestStore> idempotencyStore) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.idempotencyStore = idempotencyStore;
    }

    @Transactional
    public Notification create(String key,
            com.forge.notification.api.dto.CreateNotificationRequest request) {
        if (key == null || key.isBlank())
            throw new IllegalArgumentException("Idempotency-Key is required");
        var requestHash = RequestFingerprint.sha256(request.customerId + "|" + request.type + "|"
            + request.channel + "|" + request.priority);
        var store = idempotencyStore.getIfAvailable();
        if (store != null) {
            var completedId = store.reserve("NOTIFICATION_CREATE", key, requestHash);
            if (completedId != null) return get(completedId);
        }
        var existing = repository.findByIdempotencyKey(key);
        if (existing.isPresent())
            return existing.get();
        var notification = new Notification(UUID.fromString(request.customerId), request.type, request.channel,
                request.priority, new IdempotencyKey(key));
        repository.save(notification);
        eventPublisher.publish(new EventEnvelope("NotificationCreated", notification.getId().toString(), "Notification",
                notification.getId().toString()));
        if (store != null) store.complete("NOTIFICATION_CREATE", key, notification.getId().value());
        return notification;
    }

    public Notification get(UUID id) {
        return repository.findById(new NotificationId(id))
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    }

    public Notification cancel(UUID id) {
        var notification = get(id);
        try {
            notification.cancel();
        } catch (IllegalStateException ex) {
            throw new ConflictException(ex.getMessage());
        }
        repository.save(notification);
        return notification;
    }
}