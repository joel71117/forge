package com.forge.concurrency.lab;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

public final class AtomicExperiment {

    private final AtomicInteger integer = new AtomicInteger();
    private final AtomicLong longValue = new AtomicLong();
    private final LongAdder adder = new LongAdder();
    private final AtomicReference<JobState> jobState = new AtomicReference<>(JobState.QUEUED);

    public int incrementInteger() {
        return integer.incrementAndGet();
    }

    public long incrementLong() {
        return longValue.incrementAndGet();
    }

    public void incrementAdder() {
        adder.increment();
    }

    public int integerValue() {
        return integer.get();
    }

    public long longValue() {
        return longValue.get();
    }

    public long adderValue() {
        return adder.sum();
    }

    public boolean transition(JobState expected, JobState replacement) {
        if (!expected.canTransitionTo(replacement)) {
            return false;
        }
        return jobState.compareAndSet(expected, replacement);
    }

    public JobState jobState() {
        return jobState.get();
    }

    public enum JobState {
        QUEUED,
        RUNNING,
        COMPLETED,
        FAILED;

        private boolean canTransitionTo(JobState replacement) {
            return switch (this) {
                case QUEUED -> replacement == RUNNING || replacement == FAILED;
                case RUNNING -> replacement == COMPLETED || replacement == FAILED;
                case COMPLETED, FAILED -> false;
            };
        }
    }
}