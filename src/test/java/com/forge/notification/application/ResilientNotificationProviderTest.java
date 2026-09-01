package com.forge.notification.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.forge.commerce.common.IdempotencyKey;
import com.forge.infrastructure.resilience.Bulkhead;
import com.forge.infrastructure.resilience.CircuitBreaker;
import com.forge.notification.application.port.NotificationProvider;
import com.forge.notification.domain.Notification;
import com.forge.notification.domain.NotificationChannel;
import com.forge.notification.domain.NotificationPriority;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;

class ResilientNotificationProviderTest {
    @Test
    void opensCircuitAfterProviderFailures() {
        var executor = Executors.newSingleThreadExecutor();
        try {
            NotificationProvider failing = notification -> { throw new IllegalStateException("down"); };
            var provider = new ResilientNotificationProvider(failing, new CircuitBreaker(2, Duration.ofMinutes(1)),
                    new Bulkhead(1), executor, Duration.ofSeconds(1));
            var notification = notification();
            assertThrows(IllegalStateException.class, () -> provider.send(notification));
            assertThrows(IllegalStateException.class, () -> provider.send(notification));
            assertThrows(IllegalStateException.class, () -> provider.send(notification));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void returnsProviderReference() {
        var executor = Executors.newSingleThreadExecutor();
        try {
            var provider = new ResilientNotificationProvider(notification -> "provider-1",
                    new CircuitBreaker(2, Duration.ofMinutes(1)), new Bulkhead(1), executor, Duration.ofSeconds(1));
            assertEquals("provider-1", provider.send(notification()));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void timesOutSlowProvider() {
        var executor = Executors.newSingleThreadExecutor();
        try {
            var provider = new ResilientNotificationProvider(notification -> {
                    LockSupport.parkNanos(Duration.ofSeconds(5).toNanos());
                return "late";
            }, new CircuitBreaker(2, Duration.ofMinutes(1)), new Bulkhead(1), executor,
                    Duration.ofMillis(10));
                var notification = notification();
                assertThrows(IllegalStateException.class, () -> provider.send(notification));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void rejectsWhenBulkheadIsOccupied() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try {
            var provider = new ResilientNotificationProvider(notification -> {
                entered.countDown();
                try {
                    release.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                return "done";
            }, new CircuitBreaker(2, Duration.ofMinutes(1)), new Bulkhead(1), executor,
                    Duration.ofSeconds(1));
            var first = Executors.newSingleThreadExecutor();
            try {
                first.submit(() -> provider.send(notification()));
                entered.await();
                    var notification = notification();
                    assertThrows(IllegalStateException.class, () -> provider.send(notification));
            } finally {
                release.countDown();
                first.shutdownNow();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private Notification notification() {
        return new Notification(java.util.UUID.randomUUID(), "ORDER_CONFIRMED", NotificationChannel.EMAIL,
                NotificationPriority.NORMAL, new IdempotencyKey("test-key"));
    }
}
