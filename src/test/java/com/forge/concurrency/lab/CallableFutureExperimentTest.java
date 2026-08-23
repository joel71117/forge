package com.forge.concurrency.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

class CallableFutureExperimentTest {

    @Test
    void futureReturnsCallableResult() throws InterruptedException, ExecutionException {
        assertThat(CallableFutureExperiment.calculateGreeting("Forge"))
                .isEqualTo("Hello, Forge");
    }

    @Test
    void futureWrapsCallableFailureInExecutionException() throws InterruptedException {
        assertThat(CallableFutureExperiment.observeFailure())
                .isEqualTo("calculation failed");
    }
}