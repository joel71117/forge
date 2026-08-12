package com.forge.notification.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationTest {

    @Test
    void shouldGenerateIdAndSupportUpdates() {
        Instant now = Instant.now();
        UUID customerId = UUID.randomUUID();
        Map<String, Object> payload = Map.of("subject", "Welcome");

        Notification notification = new Notification(customerId, "WELCOME", NotificationChannel.EMAIL,
                2, "welcome-template", payload, NotificationStatus.PENDING,
                now, "key-abc", now, now);

        assertNotNull(notification.getId());
        assertEquals(NotificationStatus.PENDING, notification.getStatus());
        assertEquals("WELCOME", notification.getType());

        notification.setStatus(NotificationStatus.SENT);
        notification.setPriority(1);
        notification.setUpdatedAt(now.plusSeconds(30));

        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertEquals(1, notification.getPriority());
    }
}
