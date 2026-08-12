package com.forge.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class NotificationAttempt {
    private final NotificationAttemptId id;
    private final UUID notificationId;
    private final String provider;
    private final int attemptNumber;
    private final Instant startedAt;
    private Instant finishedAt;
    private String status;
    private String providerReference;
    private String errorCode;
    private String errorMessage;

    public NotificationAttempt(UUID notificationId, String provider, int attemptNumber, Instant startedAt,
                              Instant finishedAt, String status, String providerReference,
                              String errorCode, String errorMessage) {
        this.id = new NotificationAttemptId(UUID.randomUUID());
        this.notificationId = notificationId;
        this.provider = provider;
        this.attemptNumber = attemptNumber;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.status = status;
        this.providerReference = providerReference;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
