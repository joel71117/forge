package com.forge.concurrency.lab;

import java.util.concurrent.locks.StampedLock;

public final class StampedLockExperiment {

    private final StampedLock lock = new StampedLock();
    private int value;

    public int optimisticRead() {
        long stamp = lock.tryOptimisticRead();
        int observedValue = value;
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                observedValue = value;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return observedValue;
    }

    public long beginOptimisticRead() {
        return lock.tryOptimisticRead();
    }

    public boolean isValid(long stamp) {
        return lock.validate(stamp);
    }

    public void write(int value) {
        long stamp = lock.writeLock();
        try {
            this.value = value;
        } finally {
            lock.unlockWrite(stamp);
        }
    }
}