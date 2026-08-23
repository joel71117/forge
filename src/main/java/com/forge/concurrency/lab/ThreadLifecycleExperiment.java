package com.forge.concurrency.lab;

import java.util.concurrent.CountDownLatch;

public final class ThreadLifecycleExperiment {

    private ThreadLifecycleExperiment() {
    }

    public static Thread createRunnableWorker(CountDownLatch started, CountDownLatch release) {
        Runnable task = () -> {
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        };

        return new Thread(task, "forge-thread-lifecycle-worker");
    }

    public static String runOnCurrentThread() {
        final String[] executingThread = new String[1];
        Runnable task = () -> executingThread[0] = Thread.currentThread().getName();

        task.run();

        return executingThread[0];
    }

    public static String startOnNewThread() throws InterruptedException {
        final String[] executingThread = new String[1];
        Runnable task = () -> executingThread[0] = Thread.currentThread().getName();
        Thread worker = new Thread(task, "forge-start-worker");

        worker.start();
        worker.join();

        return executingThread[0];
    }
}