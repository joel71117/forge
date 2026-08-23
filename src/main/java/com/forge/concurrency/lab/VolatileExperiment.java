package com.forge.concurrency.lab;

import java.util.concurrent.CountDownLatch;

public final class VolatileExperiment {

    private volatile boolean running = true;
    private volatile int counter;

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public void runUntilStopped(CountDownLatch started, CountDownLatch stopped) {
        started.countDown();
        while (running) {
            Thread.onSpinWait();
        }
        stopped.countDown();
    }

    public void incrementWithForcedRace(CountDownLatch readsCompleted,
            CountDownLatch allowWrites) throws InterruptedException {
        int observedValue = counter;
        readsCompleted.countDown();
        readsCompleted.await();
        allowWrites.await();
        counter = observedValue + 1;
    }

    public int counter() {
        return counter;
    }
}