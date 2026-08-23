package com.forge.concurrency.lab;

import java.util.concurrent.CountDownLatch;

public final class UnsafeCounter {

    private int value;

    public void increment() {
        value++;
    }

    public int value() {
        return value;
    }

    public void incrementWithForcedRace(CountDownLatch readsCompleted,
            CountDownLatch allowWrites) throws InterruptedException {
        int observedValue = value;
        readsCompleted.countDown();
        readsCompleted.await();
        allowWrites.await();
        value = observedValue + 1;
    }
}