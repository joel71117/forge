package com.forge.infrastructure.observability;

import com.forge.infrastructure.events.OutboxEventStore;
import com.forge.job.infrastructure.executor.JobExecutor;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@ConditionalOnProperty(name = "forge.outbox.enabled", havingValue = "true")
public class DistributedMetrics {
    private final OutboxEventStore outbox;
    private final AtomicInteger pendingOutboxEvents = new AtomicInteger();

    public DistributedMetrics(OutboxEventStore outbox, JobExecutor jobExecutor, MeterRegistry meterRegistry) {
        this.outbox = outbox;
        Gauge.builder("forge.outbox.pending", pendingOutboxEvents::get)
                .description("Outbox events awaiting publication or recovery")
                .register(meterRegistry);
        Gauge.builder("forge.jobs.queue.depth", jobExecutor::queueDepth)
                .description("Jobs waiting for local execution")
                .register(meterRegistry);
        Gauge.builder("forge.jobs.submitted", jobExecutor::submittedCount)
                .description("Jobs submitted to the executor")
                .register(meterRegistry);
        Gauge.builder("forge.jobs.failed", jobExecutor::failedCount)
                .description("Job execution failures")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${forge.observability.poll-interval-ms:5000}")
    public void refresh() {
        pendingOutboxEvents.set(outbox.pendingCount());
    }
}