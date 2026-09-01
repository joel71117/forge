package com.forge.notification.api.dto;

import com.forge.notification.domain.*;

public record NotificationResponse(String id, String customerId, String type, NotificationChannel channel,
        NotificationPriority priority, NotificationStatus status) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId().value().toString(),
                notification.getCustomerId().toString(),
                notification.getType(), notification.getChannel(), notification.getPriority(),
                notification.getStatus());
    }
}