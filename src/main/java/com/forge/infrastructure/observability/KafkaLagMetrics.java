package com.forge.infrastructure.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "forge.kafka.consumer.enabled", havingValue = "true")
public class KafkaLagMetrics implements AutoCloseable {
    private final AdminClient adminClient;
    private final String topic;
    private final List<String> groups;
    private final Map<String, AtomicLong> lagByGroup = new ConcurrentHashMap<>();

    public KafkaLagMetrics(MeterRegistry meterRegistry,
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${forge.kafka.events-topic:forge.events}") String topic,
            @Value("${forge.kafka.consumer.groups:forge-job-workers,notification-workers,notification-delivery,order-summary-projection}") String configuredGroups) {
        this.topic = topic;
        this.groups = parseGroups(configuredGroups);
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        this.adminClient = AdminClient.create(properties);
        for (String group : groups) {
            AtomicLong lag = new AtomicLong();
            lagByGroup.put(group, lag);
            Gauge.builder("forge.kafka.consumer.lag", lag, value -> (double) value.get())
                    .description("Messages behind the end of the events topic")
                    .tag("group", group)
                    .tag("topic", topic)
                    .register(meterRegistry);
        }
    }

    @Scheduled(fixedDelayString = "${forge.observability.poll-interval-ms:5000}")
    public void refresh() {
        try {
            Map<TopicPartition, Long> endOffsets = endOffsets();
            for (String group : groups) {
                Map<TopicPartition, Long> committed = new HashMap<>();
                adminClient.listConsumerGroupOffsets(group)
                        .partitionsToOffsetAndMetadata().get()
                        .entrySet().stream()
                        .filter(entry -> entry.getKey().topic().equals(topic))
                    .forEach(entry -> committed.put(entry.getKey(), entry.getValue().offset()));
                long lag = endOffsets.entrySet().stream()
                        .mapToLong(entry -> Math.max(0, entry.getValue() - committed.getOrDefault(entry.getKey(), 0L)))
                        .sum();
                lagByGroup.get(group).set(lag);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ignored) {
            // Preserve the last known value while Kafka is unavailable.
        }
    }

    private Map<TopicPartition, Long> endOffsets() throws InterruptedException, ExecutionException {
        var partitions = adminClient.describeTopics(List.of(topic)).allTopicNames().get()
                .get(topic).partitions().stream()
                .map(partition -> new TopicPartition(topic, partition.partition()))
                .toList();
        Map<TopicPartition, OffsetSpec> requests = new HashMap<>();
        partitions.forEach(partition -> requests.put(partition, OffsetSpec.latest()));
        Map<TopicPartition, Long> result = new HashMap<>();
        adminClient.listOffsets(requests).all().get()
            .forEach((partition, offset) -> result.put(partition, offset.offset()));
        return result;
    }

    private static List<String> parseGroups(String configuredGroups) {
        if (configuredGroups == null || configuredGroups.isBlank()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (String group : configuredGroups.split(",")) {
            if (!group.isBlank()) result.add(group.trim());
        }
        return List.copyOf(result);
    }

    @Override
    public void close() {
        adminClient.close();
    }
}