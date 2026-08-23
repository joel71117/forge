package com.forge.concurrency.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

class ThreadLifecycleExperimentTest {

    @Test
    void workerMovesFromNewToRunnableToTerminated() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread worker = ThreadLifecycleExperiment.createRunnableWorker(started, release);

        assertThat(worker.getState()).isEqualTo(Thread.State.NEW);

        worker.start();
        started.await();
        assertThat(worker.getState()).isIn(Thread.State.RUNNABLE, Thread.State.WAITING);

        release.countDown();
        worker.join();
        assertThat(worker.getState()).isEqualTo(Thread.State.TERMINATED);
    }

    @Test
    void runExecutesOnCurrentThread() {
        assertThat(ThreadLifecycleExperiment.runOnCurrentThread())
                .isEqualTo(Thread.currentThread().getName());
    }

    @Test
    void startExecutesOnAnotherThread() throws InterruptedException {
        assertThat(ThreadLifecycleExperiment.startOnNewThread())
                .isEqualTo("forge-start-worker")
                .isNotEqualTo(Thread.currentThread().getName());
    }
}