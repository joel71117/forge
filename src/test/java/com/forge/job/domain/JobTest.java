package com.forge.job.domain;

import com.forge.commerce.common.IdempotencyKey;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JobTest {

    @Test
    void shouldGenerateIdAndSupportStatusMutation() {
        UUID tenantId = UUID.randomUUID();

        Job job = new Job(JobType.SEND_NOTIFICATION, tenantId, "payload", JobPriority.HIGH,
                new IdempotencyKey("idem-001"));

        assertNotNull(job.getId());
        assertEquals(JobType.SEND_NOTIFICATION, job.getType());
        assertEquals(JobStatus.QUEUED, job.getStatus());

        job.start();
        job.fail();
        job.retry();

        assertEquals(JobStatus.RETRYING, job.getStatus());
        assertEquals(1, job.getRetryCount());
    }
}
