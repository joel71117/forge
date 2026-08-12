package com.forge.job.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JobAttemptTest {

    @Test
    void shouldGenerateIdAndUpdateResult() {
        Instant started = Instant.now();
        Instant finished = started.plusSeconds(15);

        JobAttempt attempt = new JobAttempt(UUID.randomUUID(), UUID.randomUUID(), 1,
                started, finished, "RUNNING", "E-100", "Temporary failure");

        assertNotNull(attempt.getId());
        assertEquals(1, attempt.getAttemptNumber());
        assertEquals("RUNNING", attempt.getStatus());

        attempt.setStatus("SUCCEEDED");
        attempt.setErrorMessage("Recovered");
        attempt.setFinishedAt(finished.plusSeconds(5));

        assertEquals("SUCCEEDED", attempt.getStatus());
        assertEquals("Recovered", attempt.getErrorMessage());
    }
}
