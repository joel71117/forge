package com.forge.concurrency.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class ExecutorServiceExperimentTest {

    private final ExecutorServiceExperiment experiment = new ExecutorServiceExperiment();

    @Test
    void submitReturnsFutureContainingTaskResult() throws InterruptedException, ExecutionException {
        assertThat(experiment.submitAndCollect("forge")).isEqualTo("FORGE");
    }

    @Test
    void gracefulShutdownStopsAcceptingWorkAndAwaitsCompletion() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> "completed");

        assertThat(experiment.shutdownGracefully(executor, 1, TimeUnit.SECONDS)).isTrue();
        assertThat(executor.isShutdown()).isTrue();
        assertThat(executor.isTerminated()).isTrue();
    }
}