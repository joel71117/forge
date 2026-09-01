package com.forge.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "forge.kafka.tests", matches = "true")
class KafkaExperimentTest {
    private static final String BOOTSTRAP_SERVERS = System.getProperty("forge.kafka.bootstrap-servers", "localhost:9092");
    private final List<String> topics = new ArrayList<>();

    @AfterEach
    void deleteTopics() throws Exception {
        try (AdminClient admin = AdminClient.create(adminProperties())) {
            if (!topics.isEmpty()) admin.deleteTopics(topics).all().get(30, TimeUnit.SECONDS);
        }
    }

    @Test
    void publishesThousandEventsAcrossPartitionsAndMeasuresLag() throws Exception {
        String topic = createTopic(3);
        String group = "forge-lab-" + UUID.randomUUID();
        Map<Integer, AtomicInteger> producedByPartition = produce(topic, 1_000);
        assertEquals(4, producedByPartition.size());
        assertTrue(producedByPartition.values().stream().allMatch(count -> count.get() > 0));

        ConsumerRun run = consume(topic, group, 1_000, 2);
        assertEquals(1_000, run.consumed());
        assertTrue(run.maxObservedLag() > 0, "The harness must observe non-zero lag while draining");
        System.out.printf("kafka experiment: topic=%s events=1000 partitions=%s consumed=%d maxLag=%d rebalance=%s%n",
                topic, producedByPartition, run.consumed(), run.maxObservedLag(), run.rebalanceObserved());
    }

    private String createTopic(int partitions) throws Exception {
        String topic = "forge.kafka.lab." + UUID.randomUUID();
        topics.add(topic);
        try (AdminClient admin = AdminClient.create(adminProperties())) {
            admin.createTopics(List.of(new NewTopic(topic, partitions, (short) 1))).all().get(30, TimeUnit.SECONDS);
        }
        return topic;
    }

    private Map<Integer, AtomicInteger> produce(String topic, int count) throws Exception {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        Map<Integer, AtomicInteger> partitions = new HashMap<>();
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {
            for (int index = 0; index < count; index++) {
                var metadata = producer.send(new ProducerRecord<>(topic, "key-" + (index % 32), "event-" + index)).get();
                partitions.computeIfAbsent(metadata.partition(), ignored -> new AtomicInteger()).incrementAndGet();
            }
            producer.flush();
        }
        return partitions;
    }

    private ConsumerRun consume(String topic, String group, int expected, int consumerCount) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(consumerCount);
        CountDownLatch ready = new CountDownLatch(consumerCount);
        CountDownLatch consumed = new CountDownLatch(expected);
        AtomicInteger total = new AtomicInteger();
        AtomicLong maxLag = new AtomicLong(uncommittedOffsetTotal(topic));
        AtomicBoolean rebalanced = new AtomicBoolean();
        List<Consumer<String, String>> consumers = new ArrayList<>();
        try {
            for (int index = 0; index < consumerCount; index++) {
                Consumer<String, String> consumer = consumer(group);
                consumers.add(consumer);
                executor.submit(() -> consumeLoop(consumer, topic, ready, consumed, total, maxLag, rebalanced));
            }
            assertTrue(ready.await(30, TimeUnit.SECONDS));
            assertTrue(consumed.await(60, TimeUnit.SECONDS));
            return new ConsumerRun(total.get(), maxLag.get(), rebalanced.get());
        } finally {
            for (Consumer<String, String> consumer : consumers) consumer.wakeup();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
            for (Consumer<String, String> consumer : consumers) consumer.close();
        }
    }

    private void consumeLoop(Consumer<String, String> consumer, String topic, CountDownLatch ready,
            CountDownLatch consumed, AtomicInteger total, AtomicLong maxLag, AtomicBoolean rebalanced) {
        consumer.subscribe(List.of(topic), new org.apache.kafka.clients.consumer.ConsumerRebalanceListener() {
            @Override public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                // This experiment observes assignment; no offsets need revocation handling.
            }
            @Override public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                rebalanced.set(true);
            }
        });
        ready.countDown();
        try {
            while (consumed.getCount() > 0) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(250));
                if (records.isEmpty()) continue;
                records.forEach(eventRecord -> {
                    total.incrementAndGet();
                    consumed.countDown();
                });
                consumer.commitSync();
                long lag = consumer.assignment().stream()
                    .mapToLong(partition -> consumer.position(partition)
                        - consumer.committed(java.util.Set.of(partition)).get(partition).offset())
                        .sum();
                maxLag.accumulateAndGet(lag, Math::max);
            }
        } catch (org.apache.kafka.common.errors.WakeupException ignored) {
            // Normal shutdown after the expected event count is reached.
        }
    }

    private Consumer<String, String> consumer(String group) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, group);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        return new KafkaConsumer<>(properties);
    }

    private long uncommittedOffsetTotal(String topic) throws Exception {
        try (AdminClient admin = AdminClient.create(adminProperties())) {
            var partitions = admin.describeTopics(List.of(topic)).allTopicNames().get().get(topic).partitions().stream()
                    .map(partition -> new TopicPartition(topic, partition.partition())).toList();
            Map<TopicPartition, org.apache.kafka.clients.admin.OffsetSpec> requests = new HashMap<>();
            partitions.forEach(partition -> requests.put(partition, org.apache.kafka.clients.admin.OffsetSpec.latest()));
                return admin.listOffsets(requests).all().get().values().stream()
                    .mapToLong(offset -> offset.offset()).sum();
        }
    }

    private Properties adminProperties() {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        return properties;
    }

    private record ConsumerRun(int consumed, long maxObservedLag, boolean rebalanceObserved) { }
}
