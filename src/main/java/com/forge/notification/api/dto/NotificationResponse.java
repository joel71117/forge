package com.forge.notification.api.dto;

import com.forge.notification.domain.*;

public record NotificationResponse(String id, String customerId, String type, NotificationChannel channel,
                NotificationPriority priority, NotificationStatus status) {
}