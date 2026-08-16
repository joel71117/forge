package com.forge.common.application;

public interface EventPublisher {
    void publish(EventEnvelope event);
}