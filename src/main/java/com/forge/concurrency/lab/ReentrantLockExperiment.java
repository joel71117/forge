package com.forge.concurrency.lab;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

public final class ReentrantLockExperiment {

    private final ReentrantLock lock;
    private int value;

    public ReentrantLockExperiment() {
        this(false);
    }

    public ReentrantLockExperiment(boolean fair) {
        lock = new ReentrantLock(fair);
    }

    public void increment() {
        lock.lock();
        try {
            value++;
        } finally {
            lock.unlock();
        }
    }

    public boolean tryIncrement(long timeout, TimeUnit unit) throws InterruptedException {
        if (!lock.tryLock(timeout, unit)) {
            return false;
        }

        try {
            value++;
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean interruptiblyAcquire() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void holdLock(CountDownLatch lockHeld, CountDownLatch releaseRequested) {
        lock.lock();
        lockHeld.countDown();
        try {
            releaseRequested.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    public int value() {
        lock.lock();
        try {
            return value;
        } finally {
            lock.unlock();
        }
    }
}