package com.forge.notification.application;

import com.forge.common.api.ConflictException;
import com.forge.common.api.ResourceNotFoundException;
import com.forge.common.application.EventEnvelope;
import com.forge.common.application.EventPublisher;
import com.forge.commerce.common.IdempotencyKey;
import com.forge.notification.application.port.NotificationRepository;
import com.forge.notification.domain.*;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class NotificationService {
    private final NotificationRepository repository;
    private final EventPublisher eventPublisher;

    public NotificationService(NotificationRepository repository, EventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public synchronized Notification create(String key,
            com.forge.notification.api.dto.CreateNotificationRequest request) {
        if (key == null || key.isBlank())
            throw new IllegalArgumentException("Idempotency-Key is required");
        var existing = repository.findByIdempotencyKey(key);
        if (existing.isPresent())
            return existing.get();
        var notification = new Notification(UUID.fromString(request.customerId), request.type, request.channel,
                request.priority, new IdempotencyKey(key));
        repository.save(notification);
        eventPublisher.publish(new EventEnvelope("NotificationCreated", notification.getId().toString(), "Notification",
                notification.getId().toString()));
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