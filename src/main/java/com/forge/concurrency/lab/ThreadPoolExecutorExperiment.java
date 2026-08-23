package com.forge.concurrency.lab;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreadPoolExecutorExperiment {

    public ThreadPoolExecutor create(int corePoolSize, int maximumPoolSize,
            int queueCapacity, RejectedExecutionHandler rejectionHandler) {
        ThreadFactory threadFactory = new NamedThreadFactory("forge-lab-worker-");
        return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity), threadFactory, rejectionHandler);
    }

    public static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();
        private final String prefix;

        public NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable task) {
            return new Thread(task, prefix + sequence.incrementAndGet());
        }
    }

    public static RejectedExecutionHandler abortPolicy() {
        return new ThreadPoolExecutor.AbortPolicy();
    }

    public static RejectedExecutionHandler callerRunsPolicy() {
        return new ThreadPoolExecutor.CallerRunsPolicy();
    }

    public static boolean isRejected(RejectedExecutionException exception) {
        return exception != null;
    }
}