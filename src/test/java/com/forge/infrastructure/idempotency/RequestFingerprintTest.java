package com.forge.infrastructure.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RequestFingerprintTest {
    @Test
    void createsStableSha256FingerprintForRequestPayload() {
        String fingerprint = RequestFingerprint.sha256("cart=42&currency=EUR");

        assertEquals(fingerprint, RequestFingerprint.sha256("cart=42&currency=EUR"));
        assertEquals(64, fingerprint.length());
        assertTrue(fingerprint.matches("[0-9a-f]{64}"));
    }

    @Test
    void distinguishesEmptyUnicodeAndChangedPayloads() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                RequestFingerprint.sha256(""));
        assertNotEquals(RequestFingerprint.sha256("café"), RequestFingerprint.sha256("cafe"));
        assertNotEquals(RequestFingerprint.sha256("amount=10"), RequestFingerprint.sha256("amount=11"));
    }

    @Test
    void rejectsNullPayload() {
        assertThrows(NullPointerException.class, () -> RequestFingerprint.sha256(null));
    }
}