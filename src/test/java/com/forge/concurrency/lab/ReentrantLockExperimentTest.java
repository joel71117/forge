package com.forge.concurrency.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class ReentrantLockExperimentTest {

    @Test
    void tryLockTimesOutWhenAnotherThreadHoldsTheLock() throws InterruptedException {
        ReentrantLockExperiment experiment = new ReentrantLockExperiment();
        CountDownLatch lockHeld = new CountDownLatch(1);
        CountDownLatch releaseRequested = new CountDownLatch(1);
        Thread holder = new Thread(() -> experiment.holdLock(lockHeld, releaseRequested));

        holder.start();
        assertThat(lockHeld.await(1, TimeUnit.SECONDS)).isTrue();

        assertThat(experiment.tryIncrement(10, TimeUnit.MILLISECONDS)).isFalse();

        releaseRequested.countDown();
        holder.join(1_000);
        assertThat(experiment.value()).isZero();
    }

    @Test
    void lockInterruptiblyStopsWaitingWhenInterrupted() throws InterruptedException {
        ReentrantLockExperiment experiment = new ReentrantLockExperiment();
        CountDownLatch lockHeld = new CountDownLatch(1);
        CountDownLatch releaseRequested = new CountDownLatch(1);
        Thread holder = new Thread(() -> experiment.holdLock(lockHeld, releaseRequested));
        Thread waiter = new Thread(() -> assertThatThrownBy(experiment::interruptiblyAcquire)
                .isInstanceOf(InterruptedException.class));

        holder.start();
        assertThat(lockHeld.await(1, TimeUnit.SECONDS)).isTrue();
        waiter.start();
        waiter.interrupt();

        waiter.join(1_000);
        assertThat(waiter.isAlive()).isFalse();
        releaseRequested.countDown();
        holder.join(1_000);
    }

    @Test
    void lockIsReleasedAfterIncrementCompletes() {
        ReentrantLockExperiment experiment = new ReentrantLockExperiment();

        experiment.increment();

        assertThat(experiment.value()).isEqualTo(1);
    }
}