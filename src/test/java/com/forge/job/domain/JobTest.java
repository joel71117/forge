package com.forge.job.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JobTest {

    @Test
    void shouldGenerateIdAndSupportStatusMutation() {
        Instant now = Instant.now();
        UUID tenantId = UUID.randomUUID();
        Map<String, Object> payload = Map.of("orderId", "123");

        Job job = new Job("invoice-generation", tenantId, payload, 5, JobStatus.QUEUED,
                now, 0, 3, now, now, now, null, null, now);

        assertNotNull(job.getId());
        assertEquals("invoice-generation", job.getType());
        assertEquals(JobStatus.QUEUED, job.getStatus());

        job.setStatus(JobStatus.RUNNING);
        job.setRetryCount(1);
        job.setUpdatedAt(now.plusSeconds(10));

        assertEquals(JobStatus.RUNNING, job.getStatus());
        assertEquals(1, job.getRetryCount());
    }
}
