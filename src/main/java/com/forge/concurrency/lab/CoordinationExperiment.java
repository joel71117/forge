package com.forge.concurrency.lab;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;

public final class CoordinationExperiment {

    private final Semaphore providerLimit;

    public CoordinationExperiment(int maximumConcurrentCalls) {
        providerLimit = new Semaphore(maximumConcurrentCalls);
    }

    public void awaitRelease(CountDownLatch ready, CountDownLatch release)
            throws InterruptedException {
        ready.countDown();
        release.await();
    }

    public void awaitBarrier(CyclicBarrierAction action)
            throws InterruptedException, BrokenBarrierException {
        action.await();
    }

    public int advancePhase(Phaser phaser) {
        return phaser.arriveAndAwaitAdvance();
    }

    public boolean tryProviderCall() {
        if (!providerLimit.tryAcquire()) {
            return false;
        }
        try {
            return true;
        } finally {
            providerLimit.release();
        }
    }

    public boolean acquireProviderPermit() {
        return providerLimit.tryAcquire();
    }

    public void releaseProviderPermit() {
        providerLimit.release();
    }

    @FunctionalInterface
    public interface CyclicBarrierAction {
        void await() throws InterruptedException, BrokenBarrierException;
    }
}