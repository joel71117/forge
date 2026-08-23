package com.forge.concurrency.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

class CompletableFutureExperimentTest {

    @Test
    void combinesIndependentWork() {
        CompletableFutureExperiment experiment = new CompletableFutureExperiment();
        try (ExecutorService executor = Executors.newFixedThreadPool(3)) {
            assertThat(experiment.loadOrderSummary(executor).join())
                    .isEqualTo("customer:product:inventory");
        }
    }

    @Test
    void composesDependentAsyncWorkWithoutNestedFuture() {
        CompletableFutureExperiment experiment = new CompletableFutureExperiment();
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            assertThat(experiment.dependentLookup(executor).join()).isEqualTo("details-for-customer-id");
        }
    }

    @Test
    void recoversFromFailureWithExplicitFallback() {
        CompletableFutureExperiment experiment = new CompletableFutureExperiment();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            assertThat(experiment.recoverFromFailure(executor).join()).isEqualTo("fallback");
        }
    }
}