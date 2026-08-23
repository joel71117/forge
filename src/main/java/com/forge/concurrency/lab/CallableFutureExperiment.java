package com.forge.concurrency.lab;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class CallableFutureExperiment {

    private CallableFutureExperiment() {
    }

    public static String calculateGreeting(String name) throws InterruptedException, ExecutionException {
        Callable<String> task = () -> "Hello, " + name;

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<String> result = executor.submit(task);
            return result.get();
        }
    }

    public static String observeFailure() throws InterruptedException {
        Callable<String> task = () -> {
            throw new IllegalStateException("calculation failed");
        };

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<String> result = executor.submit(task);

            try {
                result.get();
                throw new AssertionError("Expected the Callable to fail");
            } catch (ExecutionException exception) {
                return exception.getCause().getMessage();
            }
        }
    }
}