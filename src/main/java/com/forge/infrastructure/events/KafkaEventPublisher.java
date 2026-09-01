package com.forge.infrastructure.events;

import com.forge.common.application.EventEnvelope;
import com.forge.common.application.EventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("'${forge.kafka.enabled:false}' == 'true' and '${forge.outbox.enabled:false}' == 'false'")
public class KafkaEventPublisher implements EventPublisher {
    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;
    private final String topic;

    public KafkaEventPublisher(KafkaTemplate<String, EventEnvelope> kafkaTemplate,
            org.springframework.core.env.Environment environment) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = environment.getProperty("forge.kafka.events-topic", "forge.events");
    }

    @Override
    public void publish(EventEnvelope event) {
        kafkaTemplate.send(topic, event.aggregateId(), event);
    }
}