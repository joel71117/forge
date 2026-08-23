package com.forge.concurrency.lab;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class ExecutorServiceExperiment {

    public String submitAndCollect(String input) throws InterruptedException, ExecutionException {
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<String> result = executor.submit((Callable<String>) input::toUpperCase);
            return result.get();
        }
    }

    public boolean shutdownGracefully(ExecutorService executor, long timeout,
            TimeUnit unit) throws InterruptedException {
        executor.shutdown();
        return executor.awaitTermination(timeout, unit);
    }
}