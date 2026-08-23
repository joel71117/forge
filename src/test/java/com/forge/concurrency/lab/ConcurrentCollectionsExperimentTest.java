package com.forge.concurrency.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class ConcurrentCollectionsExperimentTest {

    @Test
    void boundedQueueRejectsItemsWhenCapacityIsExhausted() {
        ConcurrentCollectionsExperiment<String> experiment = new ConcurrentCollectionsExperiment<>(1);

        assertThat(experiment.offer("first")).isTrue();
        assertThat(experiment.offer("second")).isFalse();
        assertThat(experiment.queueSize()).isEqualTo(1);
    }

    @Test
    void consumerCanTakeAnItemFromTheBlockingQueue() throws InterruptedException {
        ConcurrentCollectionsExperiment<String> experiment = new ConcurrentCollectionsExperiment<>(1);
        experiment.offer("job");

        assertThat(experiment.poll(1, TimeUnit.SECONDS)).isEqualTo("job");
        assertThat(experiment.queueSize()).isZero();
    }

    @Test
    void copyOnWriteCollectionProvidesStableReaderSnapshot() {
        ConcurrentCollectionsExperiment<String> experiment = new ConcurrentCollectionsExperiment<>(1);
        experiment.addSnapshot("email");
        experiment.addSnapshot("sms");

        assertThat(experiment.snapshots()).containsExactly("email", "sms");
    }
}