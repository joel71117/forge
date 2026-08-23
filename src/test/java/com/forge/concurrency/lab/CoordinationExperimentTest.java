package com.forge.concurrency.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class CoordinationExperimentTest {

    @Test
    void countDownLatchReleasesWaitingWorkerOnce() throws InterruptedException {
        CoordinationExperiment experiment = new CoordinationExperiment(1);
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread worker = new Thread(() -> awaitRelease(experiment, ready, release));

        worker.start();
        assertThat(ready.await(1, TimeUnit.SECONDS)).isTrue();
        release.countDown();
        worker.join(1_000);

        assertThat(worker.isAlive()).isFalse();
    }

    @Test
    void cyclicBarrierWaitsForEveryParty() throws InterruptedException, BrokenBarrierException {
        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch firstArrived = new CountDownLatch(1);
        Thread first = new Thread(() -> awaitBarrier(barrier, firstArrived));

        first.start();
        assertThat(firstArrived.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(first.isAlive()).isTrue();

        barrier.await();
        first.join(1_000);
        assertThat(first.isAlive()).isFalse();
    }

    @Test
    void phaserCoordinatesARegisteredPartyThroughAPhase() {
        CoordinationExperiment experiment = new CoordinationExperiment(1);
        Phaser phaser = new Phaser(1);
        int startingPhase = phaser.getPhase();

        int completedPhase = experiment.advancePhase(phaser);

        assertThat(completedPhase).isEqualTo(startingPhase + 1);
        assertThat(phaser.getPhase()).isEqualTo(startingPhase + 1);
        phaser.arriveAndDeregister();
    }

    @Test
    void semaphoreLimitsProviderConcurrency() throws InterruptedException {
        CoordinationExperiment experiment = new CoordinationExperiment(2);
        CountDownLatch permitsTaken = new CountDownLatch(2);
        CountDownLatch releasePermits = new CountDownLatch(1);
        Thread first = new Thread(() -> holdProviderPermit(experiment, permitsTaken, releasePermits));
        Thread second = new Thread(() -> holdProviderPermit(experiment, permitsTaken, releasePermits));

        first.start();
        second.start();
        assertThat(permitsTaken.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(experiment.tryProviderCall()).isFalse();

        releasePermits.countDown();
        first.join(1_000);
        second.join(1_000);
    }

    private static void awaitRelease(CoordinationExperiment experiment,
            CountDownLatch ready, CountDownLatch release) {
        try {
            experiment.awaitRelease(ready, release);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static void awaitBarrier(CyclicBarrier barrier, CountDownLatch arrived) {
        arrived.countDown();
        try {
            barrier.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (BrokenBarrierException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void holdProviderPermit(CoordinationExperiment experiment,
            CountDownLatch permitsTaken, CountDownLatch releasePermits) {
        try {
            if (experiment.acquireProviderPermit()) {
                permitsTaken.countDown();
                releasePermits.await();
                experiment.releaseProviderPermit();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}