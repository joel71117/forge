package com.forge.notification.domain;

public enum NotificationStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED,
    RETRYING,
    DEAD_LETTERED,
    CANCELLED
}
