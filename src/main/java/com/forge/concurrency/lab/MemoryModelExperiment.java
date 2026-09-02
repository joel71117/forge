package com.forge.concurrency.lab;

import java.util.concurrent.atomic.AtomicReference;

public final class MemoryModelExperiment {

    private final Object lock = new Object();
    private int synchronizedValue;
    private final AtomicReference<PublishedData> publishedData = new AtomicReference<>();

    public static int valuePublishedByStart() throws InterruptedException {
        int[] value = { 42 };
        Thread worker = new Thread(() -> assertValue(value[0]));

        worker.start();
        worker.join();
        return value[0];
    }

    public static int valuePublishedByJoin() throws InterruptedException {
        int[] value = { 0 };
        Thread worker = new Thread(() -> value[0] = 42);

        worker.start();
        worker.join();
        return value[0];
    }

    public void publishWithSynchronization(int value) {
        synchronized (lock) {
            synchronizedValue = value;
        }
    }

    public int readWithSynchronization() {
        synchronized (lock) {
            return synchronizedValue;
        }
    }

    public void publishSafely(PublishedData data) {
        publishedData.set(data);
    }

    public PublishedData readPublishedData() {
        return publishedData.get();
    }

    private static void assertValue(int value) {
        if (value != 42) {
            throw new AssertionError("Expected value to be 42 but was " + value);
        }
    }

    public record PublishedData(String name, int quantity) {
    }
}