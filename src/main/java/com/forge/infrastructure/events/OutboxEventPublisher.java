package com.forge.infrastructure.events;

import com.forge.common.application.EventEnvelope;
import com.forge.common.application.EventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "forge.outbox.enabled", havingValue = "true")
public class OutboxEventPublisher implements EventPublisher {
    private final OutboxEventStore store;

    public OutboxEventPublisher(OutboxEventStore store) {
        this.store = store;
    }

    @Override
    public void publish(EventEnvelope event) {
        store.append(event);
    }
}
