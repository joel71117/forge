package com.forge.infrastructure.resilience;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

public class Bulkhead {
    private final Semaphore permits;

    public Bulkhead(int maximumConcurrentCalls) {
        if (maximumConcurrentCalls < 1) throw new IllegalArgumentException("Maximum calls must be positive");
        permits = new Semaphore(maximumConcurrentCalls);
    }

    public <T> T execute(Supplier<T> operation) {
        if (!permits.tryAcquire()) throw new IllegalStateException("Bulkhead capacity exhausted");
        try {
            return operation.get();
        } finally {
            permits.release();
        }
    }
}
