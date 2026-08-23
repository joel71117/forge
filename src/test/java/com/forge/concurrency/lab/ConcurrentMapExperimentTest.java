package com.forge.concurrency.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class ConcurrentMapExperimentTest {

    @Test
    void checkThenActCanAllowBothThreadsToInitialize() throws InterruptedException {
        ConcurrentMapExperiment experiment = new ConcurrentMapExperiment();
        CountDownLatch checksCompleted = new CountDownLatch(2);
        CountDownLatch allowWrites = new CountDownLatch(1);
        Thread first = new Thread(() -> runUnsafe(experiment, "first", checksCompleted, allowWrites));
        Thread second = new Thread(() -> runUnsafe(experiment, "second", checksCompleted, allowWrites));

        first.start();
        second.start();
        checksCompleted.await();
        allowWrites.countDown();
        first.join();
        second.join();

        assertThat(experiment.value("key")).isIn("first", "second");
    }

    @Test
    void putIfAbsentAllowsOnlyOneWinner() {
        ConcurrentMapExperiment experiment = new ConcurrentMapExperiment();

        assertThat(experiment.putIfAbsent("key", "first")).isTrue();
        assertThat(experiment.putIfAbsent("key", "second")).isFalse();
        assertThat(experiment.value("key")).isEqualTo("first");
    }

    @Test
    void computeIfAbsentRunsInitializerOnce() {
        ConcurrentMapExperiment experiment = new ConcurrentMapExperiment();
        AtomicInteger initializerCalls = new AtomicInteger();

        assertThat(experiment.computeIfAbsent("key", initializerCalls)).isEqualTo("computed-value");
        assertThat(experiment.computeIfAbsent("key", initializerCalls)).isEqualTo("computed-value");
        assertThat(initializerCalls).hasValue(1);
    }

    private static void runUnsafe(ConcurrentMapExperiment experiment, String value,
            CountDownLatch checksCompleted, CountDownLatch allowWrites) {
        try {
            experiment.unsafeCheckThenAct("key", value, checksCompleted, allowWrites);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}