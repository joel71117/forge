package com.forge.notification.application.port;

import com.forge.notification.domain.Notification;

public interface NotificationProvider {
    String send(Notification notification);
}