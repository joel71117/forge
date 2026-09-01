package com.forge.infrastructure.resilience;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class CircuitBreaker {
    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final Duration openDuration;
    private final AtomicInteger failures = new AtomicInteger();
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private volatile Instant openedAt;

    public CircuitBreaker(int failureThreshold, Duration openDuration) {
        if (failureThreshold < 1 || openDuration.isNegative() || openDuration.isZero()) {
            throw new IllegalArgumentException("Circuit breaker settings are invalid");
        }
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
    }

    public <T> T execute(Supplier<T> operation) {
        if (!allowRequest()) throw new IllegalStateException("Circuit breaker is OPEN");
        try {
            T result = operation.get();
            state.set(State.CLOSED);
            failures.set(0);
            return result;
        } catch (RuntimeException exception) {
            if (failures.incrementAndGet() >= failureThreshold) {
                openedAt = Instant.now();
                state.set(State.OPEN);
            }
            throw exception;
        }
    }

    public State state() {
        allowRequest();
        return state.get();
    }

    private boolean allowRequest() {
        if (state.get() != State.OPEN) return true;
        if (Duration.between(openedAt, Instant.now()).compareTo(openDuration) < 0) return false;
        return state.compareAndSet(State.OPEN, State.HALF_OPEN);
    }
}
