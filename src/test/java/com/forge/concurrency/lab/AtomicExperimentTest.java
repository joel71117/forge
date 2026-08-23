package com.forge.concurrency.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

class AtomicExperimentTest {

    @Test
    void atomicCountersPreserveConcurrentUpdates() throws InterruptedException {
        AtomicExperiment experiment = new AtomicExperiment();

        runConcurrentUpdates(experiment, 100, 10_000);

        assertThat(experiment.integerValue()).isEqualTo(1_000_000);
        assertThat(experiment.longValue()).isEqualTo(1_000_000);
        assertThat(experiment.adderValue()).isEqualTo(1_000_000);
    }

    @Test
    void compareAndSetRejectsStaleStateTransition() {
        AtomicExperiment experiment = new AtomicExperiment();

        assertThat(experiment.transition(AtomicExperiment.JobState.QUEUED,
                AtomicExperiment.JobState.RUNNING)).isTrue();
        assertThat(experiment.transition(AtomicExperiment.JobState.QUEUED,
                AtomicExperiment.JobState.COMPLETED)).isFalse();
        assertThat(experiment.jobState()).isEqualTo(AtomicExperiment.JobState.RUNNING);
    }

    @Test
    void stateMachineRejectsInvalidTransitions() {
        AtomicExperiment experiment = new AtomicExperiment();

        assertThat(experiment.transition(AtomicExperiment.JobState.QUEUED,
                AtomicExperiment.JobState.COMPLETED)).isFalse();
        assertThat(experiment.jobState()).isEqualTo(AtomicExperiment.JobState.QUEUED);
    }

    private static void runConcurrentUpdates(AtomicExperiment experiment,
            int threadCount, int incrementsPerThread) throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        Thread[] workers = new Thread[threadCount];

        for (int index = 0; index < threadCount; index++) {
            workers[index] = new Thread(() -> {
                ready.countDown();
                awaitStart(start);
                for (int increment = 0; increment < incrementsPerThread; increment++) {
                    experiment.incrementInteger();
                    experiment.incrementLong();
                    experiment.incrementAdder();
                }
            });
            workers[index].start();
        }

        ready.await();
        start.countDown();
        for (Thread worker : workers) {
            worker.join();
        }
    }

    private static void awaitStart(CountDownLatch start) {
        try {
            start.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}