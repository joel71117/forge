package com.forge.notification.api.dto;

import com.forge.notification.domain.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateNotificationRequest {
    @NotNull
    public String customerId;
    @NotBlank
    public String type;
    @NotNull
    public NotificationChannel channel;
    @NotNull
    public NotificationPriority priority;
}