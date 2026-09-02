package com.forge.notification.application;

import com.forge.notification.application.port.NotificationProvider;
import com.forge.notification.application.port.NotificationRepository;
import com.forge.notification.domain.Notification;
import com.forge.notification.domain.NotificationAttempt;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "forge.kafka.consumer.enabled", havingValue = "true")
public class NotificationDeliveryService {
    private final NotificationRepository repository;
    private final NotificationProvider provider;

    public NotificationDeliveryService(NotificationRepository repository, NotificationProvider provider) {
        this.repository = repository;
        this.provider = provider;
    }

    public String deliver(UUID notificationId) {
        Notification notification = repository
                .findById(new com.forge.notification.domain.NotificationId(notificationId))
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.startProcessing();
        repository.save(notification);
        NotificationAttempt attempt = new NotificationAttempt(notificationId, "configured", 1, Instant.now());
        repository.saveAttempt(attempt);
        try {
            String reference = provider.send(notification);
            attempt.setFinishedAt(Instant.now());
            attempt.setStatus("SENT");
            attempt.setProviderReference(reference);
            repository.saveAttempt(attempt);
            notification.markSent();
            repository.save(notification);
            return reference;
        } catch (RuntimeException exception) {
            attempt.setFinishedAt(Instant.now());
            attempt.setStatus("FAILED");
            attempt.setErrorMessage(exception.getMessage());
            repository.saveAttempt(attempt);
            notification.markFailed();
            repository.save(notification);
            throw exception;
        }
    }
}
