package com.forge.notification.domain;

import com.forge.commerce.common.IdempotencyKey;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationTest {

    @Test
    void shouldGenerateIdAndSupportUpdates() {
        UUID customerId = UUID.randomUUID();

        Notification notification = new Notification(customerId, "WELCOME", NotificationChannel.EMAIL,
                NotificationPriority.NORMAL, new IdempotencyKey("key-abc"));

        assertNotNull(notification.getId());
        assertEquals(NotificationStatus.PENDING, notification.getStatus());
        assertEquals("WELCOME", notification.getType());

        notification.startProcessing();
        notification.markSent();

        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertEquals(NotificationPriority.NORMAL, notification.getPriority());
    }
}
