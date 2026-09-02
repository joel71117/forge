package com.forge.notification.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationAttemptTest {

    @Test
    void shouldGenerateIdAndRecordResult() {
        Instant started = Instant.now();
        Instant finished = started.plusSeconds(10);

        NotificationAttempt attempt = new NotificationAttempt(UUID.randomUUID(), "twilio", 1, started);
        attempt.setFinishedAt(finished);
        attempt.setStatus("FAILED");
        attempt.setProviderReference("ref-88");
        attempt.setErrorCode("E-42");
        attempt.setErrorMessage("provider timeout");

        assertNotNull(attempt.getId());
        assertEquals("twilio", attempt.getProvider());
        assertEquals("FAILED", attempt.getStatus());

        attempt.setStatus("RETRYING");
        attempt.setProviderReference("ref-99");
        attempt.setFinishedAt(finished.plusSeconds(5));

        assertEquals("RETRYING", attempt.getStatus());
        assertEquals("ref-99", attempt.getProviderReference());
    }
}
