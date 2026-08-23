package com.forge.concurrency.lab;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public final class ConcurrentCollectionsExperiment<T> {

    private final BlockingQueue<T> queue;
    private final List<T> snapshots = new CopyOnWriteArrayList<>();

    public ConcurrentCollectionsExperiment(int queueCapacity) {
        queue = new ArrayBlockingQueue<>(queueCapacity);
    }

    public boolean offer(T item) {
        return queue.offer(item);
    }

    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    public int queueSize() {
        return queue.size();
    }

    public void addSnapshot(T item) {
        snapshots.add(item);
    }

    public List<T> snapshots() {
        return List.copyOf(snapshots);
    }
}