package com.forge.concurrency.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

class UnsafeCounterTest {

    @Test
    void twoConcurrentReadModifyWriteOperationsCanLoseAnIncrement() throws InterruptedException {
        UnsafeCounter counter = new UnsafeCounter();
        CountDownLatch readsCompleted = new CountDownLatch(2);
        CountDownLatch allowWrites = new CountDownLatch(1);
        Thread first = new Thread(() -> incrementWithForcedRace(counter, readsCompleted, allowWrites));
        Thread second = new Thread(() -> incrementWithForcedRace(counter, readsCompleted, allowWrites));

        first.start();
        second.start();
        readsCompleted.await();
        allowWrites.countDown();
        first.join();
        second.join();

        assertThat(counter.value()).isEqualTo(1);
    }

    @Test
    void hundredThreadsDoNotHaveAnAtomicIncrementGuarantee() throws InterruptedException {
        UnsafeCounter counter = new UnsafeCounter();
        int threadCount = 100;
        int incrementsPerThread = 10_000;
        Thread[] workers = new Thread[threadCount];

        for (int index = 0; index < threadCount; index++) {
            workers[index] = new Thread(() -> {
                for (int increment = 0; increment < incrementsPerThread; increment++) {
                    counter.increment();
                }
            });
            workers[index].start();
        }

        for (Thread worker : workers) {
            worker.join();
        }

        assertThat(counter.value()).isLessThanOrEqualTo(threadCount * incrementsPerThread);
    }

    private static void incrementWithForcedRace(UnsafeCounter counter,
            CountDownLatch readsCompleted, CountDownLatch allowWrites) {
        try {
            counter.incrementWithForcedRace(readsCompleted, allowWrites);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}