package com.forge.concurrency.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.jupiter.api.Test;

class ThreadPoolAndVirtualThreadExperimentTest {

    @Test
    void boundedPoolRejectsWhenWorkersAndQueueAreFull() {
        ThreadPoolExecutorExperiment experiment = new ThreadPoolExecutorExperiment();
        ThreadPoolExecutor executor = experiment.create(1, 1, 1, ThreadPoolExecutorExperiment.abortPolicy());
        CountDownLatch release = new CountDownLatch(1);
        executor.submit(() -> await(release));
        executor.submit(() -> await(release));

        assertThatThrownBy(() -> executor.submit(() -> {
        })).isInstanceOf(RejectedExecutionException.class);

        release.countDown();
        executor.shutdownNow();
    }

    @Test
    void customFactoryNamesWorkers() throws Exception {
        ThreadPoolExecutorExperiment experiment = new ThreadPoolExecutorExperiment();
        ThreadPoolExecutor executor = experiment.create(1, 1, 1, ThreadPoolExecutorExperiment.abortPolicy());
        String[] name = new String[1];

        executor.submit(() -> name[0] = Thread.currentThread().getName()).get();
        executor.shutdown();

        assertThat(name[0]).startsWith("forge-lab-worker-");
    }

    @Test
    void virtualThreadsHandleBlockingTasks() throws InterruptedException {
        assertThat(new VirtualThreadExperiment().completeBlockingTasks(20, 1)).isEqualTo(20);
    }

    private static void await(CountDownLatch release) {
        try {
            release.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}