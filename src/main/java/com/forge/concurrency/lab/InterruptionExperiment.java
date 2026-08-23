package com.forge.concurrency.lab;

import java.util.concurrent.CountDownLatch;

public final class InterruptionExperiment {

    private InterruptionExperiment() {
    }

    public static Thread createCooperativelyCancellableWorker(CountDownLatch started,
            CountDownLatch cleanedUp) {
        Runnable task = () -> {
            started.countDown();

            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.onSpinWait();
                }
            } finally {
                cleanedUp.countDown();
            }
        };

        return new Thread(task, "forge-interruption-worker");
    }

    public static Thread createInterruptRestoringWorker(CountDownLatch started,
            CountDownLatch cleanedUp) {
        Runnable task = () -> {
            started.countDown();

            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                cleanedUp.countDown();
            }
        };

        return new Thread(task, "forge-interruption-restoring-worker");
    }
}