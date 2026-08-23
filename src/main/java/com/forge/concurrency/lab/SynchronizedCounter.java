package com.forge.concurrency.lab;

public final class SynchronizedCounter {

    private final Object lock = new Object();
    private int value;

    public synchronized void incrementWithMethod() {
        value++;
    }

    public void incrementWithBlock() {
        synchronized (lock) {
            value++;
        }
    }

    public synchronized int value() {
        return value;
    }
}