package com.forge.notification.application;

import com.forge.infrastructure.resilience.Bulkhead;
import com.forge.infrastructure.resilience.CircuitBreaker;
import com.forge.notification.application.port.NotificationProvider;
import com.forge.notification.domain.Notification;
import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutorService;

public class ResilientNotificationProvider implements NotificationProvider {
    private final NotificationProvider delegate;
    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;
    private final ExecutorService executor;
    private final Duration timeout;

    public ResilientNotificationProvider(NotificationProvider delegate, CircuitBreaker circuitBreaker,
            Bulkhead bulkhead, ExecutorService executor, Duration timeout) {
        this.delegate = delegate;
        this.circuitBreaker = circuitBreaker;
        this.bulkhead = bulkhead;
        this.executor = executor;
        this.timeout = timeout;
    }

    @Override
    public String send(Notification notification) {
        return bulkhead.execute(() -> circuitBreaker.execute(() -> callWithTimeout(notification)));
    }

    private String callWithTimeout(Notification notification) {
        Future<String> call = executor.submit(() -> delegate.send(notification));
        try {
            return call.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            call.cancel(true);
            throw new IllegalStateException("Notification provider timed out", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Notification provider call was interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Notification provider failed", exception);
        }
    }
}