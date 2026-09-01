package com.forge.job.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.common.application.EventEnvelope;
import com.forge.infrastructure.events.ProcessedEventStore;
import com.forge.job.application.JobService;
import com.forge.job.infrastructure.executor.JobExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "forge.kafka.consumer.enabled", havingValue = "true")
public class JobSubmittedConsumer {
    private static final String CONSUMER_NAME = "forge-job-worker";
    private static final Logger logger = LoggerFactory.getLogger(JobSubmittedConsumer.class);

    private final ObjectMapper objectMapper;
    private final ProcessedEventStore processedEvents;
    private final JobService jobService;
    private final JobExecutor executor;

    public JobSubmittedConsumer(ObjectMapper objectMapper, ProcessedEventStore processedEvents,
            JobService jobService, JobExecutor executor) {
        this.objectMapper = objectMapper;
        this.processedEvents = processedEvents;
        this.jobService = jobService;
        this.executor = executor;
    }

        @RetryableTopic(
            attempts = "4",
                backOff = @BackOff(delayString = "${forge.kafka.retry.delay-ms:1000}",
                    multiplierString = "${forge.kafka.retry.multiplier:2.0}",
                    maxDelayString = "${forge.kafka.retry.max-delay-ms:8000}",
                    jitterString = "${forge.kafka.retry.jitter-ms:250}"),
            autoCreateTopics = "true")
        @KafkaListener(topics = "${forge.kafka.events-topic}", groupId = "${forge.kafka.consumer.group-id}")
    public void consume(String message) {
        try {
            var event = objectMapper.readValue(message, EventEnvelope.class);
            if (!"JobSubmitted".equals(event.eventType())) {
                return;
            }
            processedEvents.process(event.eventId(), CONSUMER_NAME, () -> {
                executor.submit(jobService.get(UUID.fromString(event.aggregateId())));
                return "submitted";
            });
        } catch (Exception exception) {
            throw new IllegalStateException("Could not process JobSubmitted event", exception);
        }
    }

    @DltHandler
    public void deadLetter(String message, Exception exception) {
        logger.error("Job event moved to dead-letter topic; payloadLength={}", message.length(), exception);
    }
}
