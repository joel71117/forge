package com.forge.notification.application.port;

import com.forge.notification.domain.Notification;
import com.forge.notification.domain.NotificationId;
import com.forge.notification.domain.NotificationAttempt;

import java.util.Optional;

public interface NotificationRepository {
    Optional<Notification> findById(NotificationId id);

    Optional<Notification> findByIdempotencyKey(String key);

    Notification save(Notification notification);

    NotificationAttempt saveAttempt(NotificationAttempt attempt);
}