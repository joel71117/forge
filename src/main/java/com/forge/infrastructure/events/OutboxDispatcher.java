package com.forge.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.common.application.EventEnvelope;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Component
@ConditionalOnProperty(name = "forge.outbox.enabled", havingValue = "true")
public class OutboxDispatcher {
    private final OutboxEventStore store;
    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final int batchSize;

    public OutboxDispatcher(OutboxEventStore store,
            KafkaTemplate<String, EventEnvelope> kafkaTemplate,
            ObjectMapper objectMapper,
            org.springframework.core.env.Environment environment) {
        this.store = store;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = environment.getProperty("forge.kafka.events-topic", "forge.events");
        this.batchSize = environment.getProperty("forge.outbox.batch-size", Integer.class, 50);
    }

    @Scheduled(fixedDelayString = "${forge.outbox.poll-interval-ms:1000}")
    public void dispatch() {
        for (var pending : store.claim(batchSize)) {
            try {
                var envelope = objectMapper.treeToValue(pending.payload(), EventEnvelope.class);
                kafkaTemplate.send(topic, envelope.aggregateId(), envelope).get();
                store.markPublished(pending.id());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception exception) {
                var delay = retryDelay(pending.attemptCount());
                store.markFailed(pending.id(), exception, Instant.now().plus(delay));
            }
        }
    }

    private Duration retryDelay(int attempt) {
        long baseSeconds = 1L << Math.min(attempt, 4);
        long jitterMillis = ThreadLocalRandom.current().nextLong(250);
        return Duration.ofSeconds(Math.min(baseSeconds, 30)).plusMillis(jitterMillis);
    }
}
