package com.forge.concurrency.lab;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class VirtualThreadExperiment {

    public long completeBlockingTasks(int taskCount, long blockMillis) throws InterruptedException {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int index = 0; index < taskCount; index++) {
                executor.submit(() -> {
                    try {
                        Thread.sleep(blockMillis);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            executor.shutdown();
            if (!executor.awaitTermination(blockMillis + 5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Virtual-thread tasks did not finish");
            }
            return taskCount;
        }
    }
}