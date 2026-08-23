package com.forge.concurrency.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class InterruptionExperimentTest {

    @Test
    void workerStopsAfterDetectingInterrupt() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch cleanedUp = new CountDownLatch(1);
        Thread worker = InterruptionExperiment.createCooperativelyCancellableWorker(started, cleanedUp);

        worker.start();
        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

        worker.interrupt();

        assertThat(cleanedUp.await(1, TimeUnit.SECONDS)).isTrue();
        worker.join(1_000);
        assertThat(worker.isAlive()).isFalse();
    }

    @Test
    void interruptedExceptionClearsFlagUnlessWorkerRestoresIt() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch cleanedUp = new CountDownLatch(1);
        Thread worker = InterruptionExperiment.createInterruptRestoringWorker(started, cleanedUp);

        worker.start();
        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

        worker.interrupt();

        assertThat(cleanedUp.await(1, TimeUnit.SECONDS)).isTrue();
        worker.join(1_000);
        assertThat(worker.isAlive()).isFalse();
        assertThat(worker.isInterrupted()).isTrue();
    }
}