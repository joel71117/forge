package com.forge.concurrency.lab;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.CountDownLatch;

public final class ReadWriteLockExperiment {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private int value;

    public int read() {
        lock.readLock().lock();
        try {
            return value;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void write(int value) {
        lock.writeLock().lock();
        try {
            this.value = value;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void holdReadLock(CountDownLatch lockHeld, CountDownLatch releaseRequested) {
        lock.readLock().lock();
        lockHeld.countDown();
        try {
            releaseRequested.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            lock.readLock().unlock();
        }
    }
}