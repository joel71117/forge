package com.forge.infrastructure.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.forge.infrastructure.events.OutboxEventStore;
import com.forge.job.infrastructure.executor.JobExecutor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class DistributedMetricsTest {
    @Test
    void registersExecutorGaugesAndRefreshesPendingOutboxCount() {
        OutboxEventStore outbox = mock(OutboxEventStore.class);
        JobExecutor executor = mock(JobExecutor.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        when(outbox.pendingCount()).thenReturn(7);
        when(executor.queueDepth()).thenReturn(3);
        when(executor.submittedCount()).thenReturn(12L);
        when(executor.failedCount()).thenReturn(2L);

        new DistributedMetrics(outbox, executor, registry).refresh();

        assertEquals(7.0, registry.get("forge.outbox.pending").gauge().value());
        assertEquals(3.0, registry.get("forge.jobs.queue.depth").gauge().value());
        assertEquals(12.0, registry.get("forge.jobs.submitted").gauge().value());
        assertEquals(2.0, registry.get("forge.jobs.failed").gauge().value());
    }
}