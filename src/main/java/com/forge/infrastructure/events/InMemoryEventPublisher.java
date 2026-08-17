package com.forge.infrastructure.events;

import com.forge.common.application.EventEnvelope;
import com.forge.common.application.EventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryEventPublisher implements EventPublisher {
    private final List<EventEnvelope> events = new CopyOnWriteArrayList<>();

    @Override
    public void publish(EventEnvelope event) {
        events.add(event);
    }

    public List<EventEnvelope> events() {
        return List.copyOf(events);
    }
}