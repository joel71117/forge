package com.forge.concurrency.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

class SynchronizedCounterTest {

    @Test
    void synchronizedMethodPreservesConcurrentIncrements() throws InterruptedException {
        SynchronizedCounter counter = new SynchronizedCounter();

        runConcurrentIncrements(counter::incrementWithMethod);

        assertThat(counter.value()).isEqualTo(1_000_000);
    }

    @Test
    void synchronizedBlockPreservesConcurrentIncrements() throws InterruptedException {
        SynchronizedCounter counter = new SynchronizedCounter();

        runConcurrentIncrements(counter::incrementWithBlock);

        assertThat(counter.value()).isEqualTo(1_000_000);
    }

    private static void runConcurrentIncrements(Runnable increment) throws InterruptedException {
        int threadCount = 100;
        int incrementsPerThread = 10_000;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        Thread[] workers = new Thread[threadCount];

        for (int index = 0; index < threadCount; index++) {
            workers[index] = new Thread(() -> {
                ready.countDown();
                awaitStart(start);
                for (int incrementCount = 0; incrementCount < incrementsPerThread; incrementCount++) {
                    increment.run();
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