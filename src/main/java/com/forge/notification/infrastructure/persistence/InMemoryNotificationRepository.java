package com.forge.notification.infrastructure.persistence;

import com.forge.notification.application.port.NotificationRepository;
import com.forge.notification.domain.Notification;
import com.forge.notification.domain.NotificationAttempt;
import com.forge.notification.domain.NotificationId;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("!local")
public class InMemoryNotificationRepository implements NotificationRepository {
    private final ConcurrentHashMap<NotificationId, Notification> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, NotificationAttempt> attempts = new ConcurrentHashMap<>();

    @Override
    public Optional<Notification> findById(NotificationId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Notification> findByIdempotencyKey(String key) {
        return store.values().stream().filter(n -> n.getIdempotencyKey().value().equals(key)).findFirst();
    }

    @Override
    public Notification save(Notification notification) {
        store.put(notification.getId(), notification);
        return notification;
    }

    @Override
    public NotificationAttempt saveAttempt(NotificationAttempt attempt) {
        attempts.put(attempt.getId().value(), attempt);
        return attempt;
    }
}