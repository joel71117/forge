package com.forge.concurrency.lab;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public final class ConcurrentMapExperiment {

    private final Map<String, String> values = new ConcurrentHashMap<>();

    public boolean unsafeCheckThenAct(String key, String value,
            CountDownLatch checksCompleted, CountDownLatch allowWrites) throws InterruptedException {
        if (!values.containsKey(key)) {
            checksCompleted.countDown();
            checksCompleted.await();
            allowWrites.await();
            values.put(key, value);
            return true;
        }
        return false;
    }

    public boolean putIfAbsent(String key, String value) {
        return values.putIfAbsent(key, value) == null;
    }

    public String computeIfAbsent(String key, AtomicInteger initializerCalls) {
        return values.computeIfAbsent(key, ignored -> {
            initializerCalls.incrementAndGet();
            return "computed-value";
        });
    }

    public String value(String key) {
        return values.get(key);
    }
}