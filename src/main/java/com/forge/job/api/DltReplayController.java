package com.forge.job.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/admin/dlt")
@ConditionalOnProperty(name = "forge.kafka.consumer.enabled", havingValue = "true")
public class DltReplayController {
    private static final Logger logger = LoggerFactory.getLogger(DltReplayController.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String eventsTopic;

    public DltReplayController(KafkaTemplate<String, String> kafkaTemplate,
            @Value("${forge.kafka.events-topic:forge.events}") String eventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventsTopic = eventsTopic;
    }

    @PostMapping("/replay")
    public ReplayResponse replay(@RequestBody String payload,
            @RequestHeader("X-Admin-Actor") String actor,
            @RequestHeader("X-Replay-Reason") String reason) {
        if (payload == null || payload.isBlank()) throw new IllegalArgumentException("DLT payload is required");
        if (actor == null || actor.isBlank()) throw new IllegalArgumentException("X-Admin-Actor is required");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("X-Replay-Reason is required");
        kafkaTemplate.send(eventsTopic, payload);
        logger.warn("DLT replay requested actor={} reason={} destination={} payloadLength={}", actor, reason,
                eventsTopic, payload.length());
        return new ReplayResponse(eventsTopic, actor, reason);
    }

    public record ReplayResponse(String destination, String actor, String reason) {
    }
}