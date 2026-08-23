package com.forge.concurrency.lab;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StampedLockExperimentTest {

    @Test
    void optimisticReadReturnsCurrentValue() {
        StampedLockExperiment experiment = new StampedLockExperiment();
        experiment.write(42);

        assertThat(experiment.optimisticRead()).isEqualTo(42);
    }

    @Test
    void writeInvalidatesAnEarlierOptimisticReadStamp() {
        StampedLockExperiment experiment = new StampedLockExperiment();
        long stamp = experiment.beginOptimisticRead();

        experiment.write(42);

        assertThat(experiment.isValid(stamp)).isFalse();
    }
}