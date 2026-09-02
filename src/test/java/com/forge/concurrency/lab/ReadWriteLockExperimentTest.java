package com.forge.concurrency.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class ReadWriteLockExperimentTest {

    @Test
    void readersCanHoldReadLockTogether() throws InterruptedException {
        ReadWriteLockExperiment experiment = new ReadWriteLockExperiment();
        CountDownLatch firstReaderStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstReader = new CountDownLatch(1);
        Thread firstReader = new Thread(() -> experiment.holdReadLock(firstReaderStarted, releaseFirstReader));

        firstReader.start();
        assertThat(firstReaderStarted.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(experiment.read()).isZero();

        releaseFirstReader.countDown();
        firstReader.join(1_000);
    }

    @Test
    void writerPublishesValueThroughExclusiveWriteLock() {
        ReadWriteLockExperiment experiment = new ReadWriteLockExperiment();

        experiment.write(42);

        assertThat(experiment.read()).isEqualTo(42);
    }
}