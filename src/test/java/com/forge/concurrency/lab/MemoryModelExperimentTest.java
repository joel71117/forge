package com.forge.concurrency.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

class MemoryModelExperimentTest {

    @Test
    void actionsBeforeStartAreVisibleToStartedThread() throws InterruptedException {
        assertThat(MemoryModelExperiment.valuePublishedByStart()).isEqualTo(42);
    }

    @Test
    void actionsInWorkerAreVisibleAfterJoin() throws InterruptedException {
        assertThat(MemoryModelExperiment.valuePublishedByJoin()).isEqualTo(42);
    }

    @Test
    void unlockAndLaterLockProvideAVisibilityHandoff() throws InterruptedException {
        MemoryModelExperiment experiment = new MemoryModelExperiment();
        CountDownLatch published = new CountDownLatch(1);
        Thread publisher = new Thread(() -> {
            experiment.publishWithSynchronization(42);
            published.countDown();
        });

        publisher.start();
        published.await();
        publisher.join();

        assertThat(experiment.readWithSynchronization()).isEqualTo(42);
    }

    @Test
    void volatileReferenceSafelyPublishesImmutableData() {
        MemoryModelExperiment experiment = new MemoryModelExperiment();

        experiment.publishSafely(new MemoryModelExperiment.PublishedData("widget", 3));

        assertThat(experiment.readPublishedData())
                .isEqualTo(new MemoryModelExperiment.PublishedData("widget", 3));
    }
}