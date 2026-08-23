package com.forge.concurrency.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class VolatileExperimentTest {

    @Test
    void volatileStopFlagMakesShutdownVisibleToWorker() throws InterruptedException {
        VolatileExperiment experiment = new VolatileExperiment();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch stopped = new CountDownLatch(1);
        Thread worker = new Thread(() -> experiment.runUntilStopped(started, stopped));

        worker.start();
        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
        experiment.stop();

        assertThat(stopped.await(1, TimeUnit.SECONDS)).isTrue();
        worker.join(1_000);
        assertThat(worker.isAlive()).isFalse();
        assertThat(experiment.isRunning()).isFalse();
    }

    @Test
    void volatileDoesNotMakeIncrementAtomic() throws InterruptedException {
        VolatileExperiment experiment = new VolatileExperiment();
        CountDownLatch readsCompleted = new CountDownLatch(2);
        CountDownLatch allowWrites = new CountDownLatch(1);
        Thread first = createIncrementWorker(experiment, readsCompleted, allowWrites);
        Thread second = createIncrementWorker(experiment, readsCompleted, allowWrites);

        first.start();
        second.start();
        readsCompleted.await();
        allowWrites.countDown();
        first.join();
        second.join();

        assertThat(experiment.counter()).isEqualTo(1);
    }

    private static Thread createIncrementWorker(VolatileExperiment experiment,
            CountDownLatch readsCompleted, CountDownLatch allowWrites) {
        return new Thread(() -> {
            try {
                experiment.incrementWithForcedRace(readsCompleted, allowWrites);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
    }
}